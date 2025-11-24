package com.swe.screenNVideo;

import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

class SubscriberPacketTest {

    @Test
    void testSerialize() {
        // basic serialization check
        String ip = "192.168.1.10";
        boolean reqCompression = true;
        SubscriberPacket packet = new SubscriberPacket(ip, reqCompression);

        byte[] data = packet.serialize();

        // expected length: 1 + 4*4 + 1 = 18
        assertEquals(18, data.length);

        ByteBuffer buffer = ByteBuffer.wrap(data);

        // dummy byte
        assertEquals((byte) 0, buffer.get());

        // IP octets
        assertEquals(192, buffer.getInt());
        assertEquals(168, buffer.getInt());
        assertEquals(1, buffer.getInt());
        assertEquals(10, buffer.getInt());

        // compression flag
        assertEquals((byte) 1, buffer.get());
    }

    @Test
    void testSerializeWithoutCompression() {
        SubscriberPacket packet = new SubscriberPacket("10.0.0.1", false);
        byte[] data = packet.serialize();

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(17); // last byte is compression flag
        assertEquals((byte) 0, buffer.get());
    }
}
