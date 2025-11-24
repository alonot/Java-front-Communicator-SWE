package com.swe.ux.viewmodel;

import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.screenNVideo.Utils;
import com.swe.ux.model.UIImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ScreenNVideoModelTest {

    private FakeRPC fakeRpc;

    static class FakeRPC implements AbstractRPC {
        List<String> calledMethods = new ArrayList<>();
        List<byte[]> calledData = new ArrayList<>();
        Map<String, Function<byte[], byte[]>> subscribers = new HashMap<>();

        @Override
        public void subscribe(String methodName, Function<byte[], byte[]> method) {
            subscribers.put(methodName, method);
        }

        @Override
        public Thread connect(int portNumber) { return null; }

        @Override
        public CompletableFuture<byte[]> call(String methodName, byte[] data) {
            calledMethods.add(methodName);
            calledData.add(data);
            return CompletableFuture.completedFuture(new byte[0]);
        }

        public int countCalls(String methodName) {
            int count = 0;
            for (String m : calledMethods) {
                if (m.equals(methodName)) count++;
            }
            return count;
        }
    }

    @BeforeEach
    void setup() throws Exception {
        fakeRpc = new FakeRPC();

        // reset singleton
        Field instance = ScreenNVideoModel.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    void testSingleton() {
        ScreenNVideoModel model1 = ScreenNVideoModel.getInstance(fakeRpc);
        ScreenNVideoModel model2 = ScreenNVideoModel.getInstance(fakeRpc);
        assertSame(model1, model2);
    }

    @Test
    void testRequestMethods() {
        ScreenNVideoModel model = ScreenNVideoModel.getInstance(fakeRpc);
        String ip = "1.2.3.4";

        // uncompressed
        model.requestUncompressedData(ip);
        assertEquals(1, fakeRpc.countCalls(Utils.SUBSCRIBE_AS_VIEWER));
        byte[] data1 = fakeRpc.calledData.get(0);
        assertEquals(0, data1[data1.length - 1]);

        fakeRpc.calledMethods.clear();
        fakeRpc.calledData.clear();

        // compressed
        model.requestCompressedData(ip);
        assertEquals(1, fakeRpc.countCalls(Utils.SUBSCRIBE_AS_VIEWER));
        byte[] data2 = fakeRpc.calledData.get(0);
        assertEquals(1, data2[data2.length - 1]);
    }

    @Test
    void testUpdateVisibleParticipants() {
        ScreenNVideoModel model = ScreenNVideoModel.getInstance(fakeRpc);

        Set<String> visible = new HashSet<>();
        visible.add("10.0.0.1");

        model.updateVisibleParticipants(visible);

        // subscribe happened and state updated
        assertTrue(fakeRpc.countCalls(Utils.SUBSCRIBE_AS_VIEWER) >= 1);
        assertTrue(model.visibleParticipants.get().contains("10.0.0.1"));

        fakeRpc.calledMethods.clear();

        // remove participant
        Set<String> empty = new HashSet<>();
        model.updateVisibleParticipants(empty);

        assertTrue(fakeRpc.countCalls(Utils.UNSUBSCRIBE_AS_VIEWER) >= 1);
        assertTrue(model.visibleParticipants.get().isEmpty());
    }

    @Test
    void testRedundantUpdatesAndOverlaps() {
        ScreenNVideoModel model = ScreenNVideoModel.getInstance(fakeRpc);

        String userA = "10.0.0.1";
        String userB = "10.0.0.2";

        Set<String> set1 = new HashSet<>(Collections.singletonList(userA));
        model.updateVisibleParticipants(set1);
        fakeRpc.calledMethods.clear();

        Set<String> set2 = new HashSet<>(Arrays.asList(userA, userB));
        model.updateVisibleParticipants(set2);

        // only subscribe for the new userB
        assertEquals(1, fakeRpc.countCalls(Utils.SUBSCRIBE_AS_VIEWER), "Should only subscribe to UserB");
        assertEquals(0, fakeRpc.countCalls(Utils.UNSUBSCRIBE_AS_VIEWER), "Should not unsubscribe retained UserA");

        assertTrue(model.visibleParticipants.get().contains(userA));
        assertTrue(model.visibleParticipants.get().contains(userB));
    }

    @Test
    void testInvalidIPHandling() {
        ScreenNVideoModel model = ScreenNVideoModel.getInstance(fakeRpc);

        Set<String> invalidIps = new HashSet<>();
        invalidIps.add("invalid.ip.string");

        // adding invalid IP should not throw
        assertDoesNotThrow(() -> model.updateVisibleParticipants(invalidIps));

        // no RPC calls due to serialization failure
        assertEquals(0, fakeRpc.calledMethods.size());

        // local set updated
        assertTrue(model.visibleParticipants.get().contains("invalid.ip.string"));

        // removing invalid IP should not throw
        assertDoesNotThrow(() -> model.updateVisibleParticipants(new HashSet<>()));
        assertEquals(0, fakeRpc.calledMethods.size());
    }

    @Test
    void testImageReceptionCallback() {
        ScreenNVideoModel model = ScreenNVideoModel.getInstance(fakeRpc);

        List<UIImage> receivedImages = new ArrayList<>();
        model.setOnImageReceived(receivedImages::add);

        // ensure subscription exists
        assertTrue(fakeRpc.subscribers.containsKey(Utils.UPDATE_UI));

        // build RImage-like payload
        String testIp = "192.168.1.50";
        byte[] ipBytes = testIp.getBytes(StandardCharsets.UTF_8);
        int width = 2;
        int height = 2;

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(ipBytes.length);
        buffer.put(ipBytes);
        buffer.putInt(height);
        buffer.putInt(width);

        // pixels: R, G, B triplets
        buffer.put((byte)255).put((byte)0).put((byte)0);   // red
        buffer.put((byte)0).put((byte)255).put((byte)0);   // green
        buffer.put((byte)0).put((byte)0).put((byte)255);   // blue
        buffer.put((byte)255).put((byte)255).put((byte)255); // white

        byte[] payload = Arrays.copyOf(buffer.array(), buffer.position());

        // trigger callback
        Function<byte[], byte[]> callback = fakeRpc.subscribers.get(Utils.UPDATE_UI);
        byte[] response = callback.apply(payload);

        // verify
        assertEquals(1, response.length);
        assertEquals(1, response[0]);

        assertEquals(1, receivedImages.size());
        UIImage result = receivedImages.get(0);
        assertEquals(testIp, result.ip());
        assertNotNull(result.image());
        assertEquals(width, result.image().getWidth());
        assertEquals(height, result.image().getHeight());
    }
}
