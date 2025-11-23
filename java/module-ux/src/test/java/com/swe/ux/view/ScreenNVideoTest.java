package com.swe.ux.view;

import com.swe.controller.Meeting.ParticipantRole;
import com.swe.controller.Meeting.UserProfile;
import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.screenNVideo.Utils;
import com.swe.ux.model.UIImage;
import com.swe.ux.viewmodel.MeetingViewModel;
import com.swe.ux.viewmodel.ScreenNVideoModel;
import com.swe.ux.ui.ParticipantPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ScreenNVideoTest {

    @BeforeAll
    static void setupHeadless() {
        // run AWT in headless mode for CI
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void resetStaticState() throws Exception {
        // clear singletons and static maps before each test
        Field instance = ScreenNVideoModel.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, null);

        Field updating = ScreenNVideo.class.getDeclaredField("updating");
        updating.setAccessible(true);
        AtomicBoolean updatingBool = (AtomicBoolean) updating.get(null);
        if (updatingBool != null) {
            updatingBool.set(false);
        }

        Field panels = ScreenNVideo.class.getDeclaredField("participantPanels");
        panels.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) panels.get(null);
        if (map != null) {
            map.clear();
        }
    }

    static class FakeRPC implements AbstractRPC {
        Map<String, Function<byte[], byte[]>> subscribers = new HashMap<>();

        @Override
        public void subscribe(String methodName, Function<byte[], byte[]> method) {
            subscribers.put(methodName, method);
        }

        @Override
        public Thread connect(int portNumber) { return null; }

        @Override
        public CompletableFuture<byte[]> call(String methodName, byte[] data) {
            return CompletableFuture.completedFuture(new byte[0]);
        }
    }

    // helper to process EDT events
    private void flushEDT() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {});
    }

    // reflection helpers
    private JScrollPane getScrollPane(ScreenNVideo view) throws Exception {
        Field f = ScreenNVideo.class.getDeclaredField("scrollPane");
        f.setAccessible(true);
        return (JScrollPane) f.get(view);
    }

    private String getZoomedIp(ScreenNVideo view) throws Exception {
        Field f = ScreenNVideo.class.getDeclaredField("zoomedParticipantIp");
        f.setAccessible(true);
        return (String) f.get(view);
    }

    private JPanel getVideoGrid(ScreenNVideo view) throws Exception {
        Field f = ScreenNVideo.class.getDeclaredField("videoGrid");
        f.setAccessible(true);
        return (JPanel) f.get(view);
    }

    private Map<String, ParticipantPanel> getParticipantPanels() throws Exception {
        Field f = ScreenNVideo.class.getDeclaredField("participantPanels");
        f.setAccessible(true);
        return (Map<String, ParticipantPanel>) f.get(null);
    }

    private void resizeViewport(JScrollPane scrollPane, int w, int h) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            scrollPane.getViewport().setSize(w, h);
            for(ComponentListener cl : scrollPane.getViewport().getComponentListeners()) {
                cl.componentResized(new ComponentEvent(scrollPane.getViewport(), ComponentEvent.COMPONENT_RESIZED));
            }
        });
    }

    @Test
    void testSwapZoom_OldPanelNull() throws Exception {
        // ensure swap works when old zoomed panel is missing
        FakeRPC fakeRpc = new FakeRPC();
        UserProfile currentUser = new UserProfile("me@test.com", "Me", "url", ParticipantRole.STUDENT);
        MeetingViewModel meetingVM = new MeetingViewModel(currentUser, fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        flushEDT();

        // Add users
        List<UserProfile> users = new ArrayList<>();
        users.add(new UserProfile("u1", "U1", null, ParticipantRole.STUDENT));
        users.get(0).setIp("1.1.1.1");
        users.add(new UserProfile("u2", "U2", null, ParticipantRole.STUDENT));
        users.get(1).setIp("1.1.1.2");
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        // Inject "Ghost" Zoom State (IP set, but Panel missing from map)
        Field zoomedIpField = ScreenNVideo.class.getDeclaredField("zoomedParticipantIp");
        zoomedIpField.setAccessible(true);
        zoomedIpField.set(screenNVideo, "ghost.ip");

        // Invoke swapZoom
        Method swapZoom = ScreenNVideo.class.getDeclaredMethod("swapZoom", String.class);
        swapZoom.setAccessible(true);

        SwingUtilities.invokeAndWait(() -> {
            try { swapZoom.invoke(screenNVideo, "1.1.1.2"); } catch (Exception e) {}
        });
        flushEDT();

        // Verify we swapped to 1.1.1.2 despite old panel being null
        assertEquals("1.1.1.2", getZoomedIp(screenNVideo));
    }

    @Test
    void testSwapZoom_OldPanelNotNull() throws Exception {
        // ensure swap works when old zoomed panel exists
        FakeRPC fakeRpc = new FakeRPC();
        UserProfile currentUser = new UserProfile("me@test.com", "Me", "url", ParticipantRole.STUDENT);
        MeetingViewModel meetingVM = new MeetingViewModel(currentUser, fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        flushEDT();

        List<UserProfile> users = new ArrayList<>();
        users.add(new UserProfile("u1", "U1", null, ParticipantRole.STUDENT));
        users.get(0).setIp("1.1.1.1");
        users.add(new UserProfile("u2", "U2", null, ParticipantRole.STUDENT));
        users.get(1).setIp("1.1.1.2");
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        // Zoom normally to set valid state
        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.1.1.1"));
        flushEDT();

        // Invoke swapZoom via reflection
        Method swapZoom = ScreenNVideo.class.getDeclaredMethod("swapZoom", String.class);
        swapZoom.setAccessible(true);

        SwingUtilities.invokeAndWait(() -> {
            try { swapZoom.invoke(screenNVideo, "1.1.1.2"); } catch (Exception e) {}
        });
        flushEDT();

        assertEquals("1.1.1.2", getZoomedIp(screenNVideo));
    }

    @Test
    void testFullLifecycle() throws Exception {
        // full UI lifecycle smoke test
        FakeRPC fakeRpc = new FakeRPC();
        UserProfile currentUser = new UserProfile("me@test.com", "Me", "url", ParticipantRole.STUDENT);
        MeetingViewModel meetingVM = new MeetingViewModel(currentUser, fakeRpc);

        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        assertNotNull(screenNVideo);
        flushEDT();

        List<UserProfile> users = new ArrayList<>();
        users.add(new UserProfile("u1@test.com", "User 1", null, ParticipantRole.STUDENT));
        users.get(0).setIp("1.1.1.1");

        // Add user
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        // Add Duplicate
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        // Add second user
        users.add(new UserProfile("u2@test.com", "User 2", null, ParticipantRole.STUDENT));
        users.get(1).setIp("1.1.1.2");
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        // Zoom In
        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.1.1.1"));
        flushEDT();
        assertEquals("1.1.1.1", getZoomedIp(screenNVideo));

        // Swap Zoom
        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.1.1.2"));
        flushEDT();
        assertEquals("1.1.1.2", getZoomedIp(screenNVideo));

        // Zoom Out
        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.1.1.2"));
        flushEDT();
        assertNull(getZoomedIp(screenNVideo));

        // Frame Logic
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        UIImage validFrame = new UIImage(img, "1.1.1.1", (byte) 1);
        ScreenNVideo.displayFrame(validFrame);
        flushEDT();

        // Stop Share Callback
        if (fakeRpc.subscribers.containsKey(Utils.STOP_SHARE)) {
            fakeRpc.subscribers.get(Utils.STOP_SHARE).apply("1.1.1.1".getBytes());
        }
        flushEDT();

        // Remove Zoomed User
        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.1.1.1"));
        flushEDT();
        users.remove(0);
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();
        assertNull(getZoomedIp(screenNVideo));
    }

    @Test
    void testZoomLogicEdgeCases() throws Exception {
        // edge cases for zoom behavior
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        flushEDT();

        // 1. Zoom ignored with only 1 user
        List<UserProfile> users = new ArrayList<>();
        users.add(new UserProfile("u1", "U1", null, ParticipantRole.STUDENT));
        users.get(0).setIp("1.1.1.1");
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.1.1.1"));
        flushEDT();
        assertNull(getZoomedIp(screenNVideo));

        // 2. Add second user
        users.add(new UserProfile("u2", "U2", null, ParticipantRole.STUDENT));
        users.get(1).setIp("1.1.1.2");
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        // 3. Zoom Guard: panel == null (Non-existent IP)
        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("9.9.9.9"));
        flushEDT();
        assertNull(getZoomedIp(screenNVideo));

        // 4. Swap Zoom Guard
        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.1.1.1"));
        flushEDT();

        // Try swap to invalid
        Method swapZoom = ScreenNVideo.class.getDeclaredMethod("swapZoom", String.class);
        swapZoom.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> {
            try { swapZoom.invoke(screenNVideo, "9.9.9.9"); } catch (Exception e) { throw new RuntimeException(e); }
        });
        flushEDT();
        assertEquals("1.1.1.1", getZoomedIp(screenNVideo));

        // 5. Zoom Out Guard
        Method zoomOut = ScreenNVideo.class.getDeclaredMethod("zoomOut");
        zoomOut.setAccessible(true);
        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.1.1.2")); // Reset
        assertDoesNotThrow(() -> zoomOut.invoke(screenNVideo));
    }

    @Test
    void testLayoutLogicAndConstraints() throws Exception {
        // layout and resizing checks
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        JScrollPane scrollPane = getScrollPane(screenNVideo);

        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>()));
        resizeViewport(scrollPane, 500, 500);
        flushEDT();

        List<UserProfile> users = new ArrayList<>();
        for(int i=0; i<5; i++) {
            UserProfile u = new UserProfile("u"+i, "U"+i, null, ParticipantRole.STUDENT);
            u.setIp("1.0.0."+i);
            users.add(u);
        }
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        resizeViewport(scrollPane, 0, 0);
        flushEDT();
        resizeViewport(scrollPane, 100, 0);
        flushEDT();
        resizeViewport(scrollPane, 0, 100);
        flushEDT();

        resizeViewport(scrollPane, 500, 800);
        flushEDT();
        resizeViewport(scrollPane, 700, 800);
        flushEDT();
        resizeViewport(scrollPane, 1000, 800);
        flushEDT();

        resizeViewport(scrollPane, 1000, 100);
        flushEDT();
        resizeViewport(scrollPane, 400, 2000);
        flushEDT();

        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.0.0.0"));
        flushEDT();
        resizeViewport(scrollPane, 800, 600);
        flushEDT();
    }

    @Test
    void testVisibilityIntersectionExplicit() throws Exception {
        // test which panels are visible in the viewport
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        JScrollPane scrollPane = getScrollPane(screenNVideo);
        JPanel videoGrid = getVideoGrid(screenNVideo);
        ScreenNVideoModel model = ScreenNVideoModel.getInstance(fakeRpc);

        List<UserProfile> users = new ArrayList<>();
        users.add(new UserProfile("u1", "U1", null, ParticipantRole.STUDENT));
        users.get(0).setIp("1.1.1.1");
        users.add(new UserProfile("u2", "U2", null, ParticipantRole.STUDENT));
        users.get(1).setIp("2.2.2.2");
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        SwingUtilities.invokeAndWait(() -> {
            videoGrid.setLayout(null);
            videoGrid.setPreferredSize(new Dimension(200, 600));
            videoGrid.setSize(200, 600);

            scrollPane.getViewport().setSize(200, 200);
            Component[] comps = videoGrid.getComponents();
            if (comps.length >= 2) {
                comps[0].setBounds(0, 0, 200, 200);
                comps[1].setBounds(0, 300, 200, 200);
            }
        });
        flushEDT();

        SwingUtilities.invokeAndWait(() -> scrollPane.getViewport().setViewPosition(new Point(0, 0)));
        flushEDT();
        assertTrue(model.visibleParticipants.get().contains("1.1.1.1"));
        assertFalse(model.visibleParticipants.get().contains("2.2.2.2"));

        SwingUtilities.invokeAndWait(() -> scrollPane.getViewport().setViewPosition(new Point(0, 300)));
        flushEDT();
        assertFalse(model.visibleParticipants.get().contains("1.1.1.1"));
        assertTrue(model.visibleParticipants.get().contains("2.2.2.2"));
    }

    @Test
    void testNullifyImageMissingPanel() throws Exception {
        // nullifyImage should log when panel missing
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        flushEDT();

        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));

        try {
            SwingUtilities.invokeAndWait(() -> screenNVideo.nullifyImage("99.99.99.99"));
            flushEDT();
            assertTrue(errContent.toString().contains("No active participant panel initialized"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void testZoomOutGuardViaReflection() throws Exception {
        // ensure zoomOut doesn't crash
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        Method zoomOut = ScreenNVideo.class.getDeclaredMethod("zoomOut");
        zoomOut.setAccessible(true);
        assertDoesNotThrow(() -> zoomOut.invoke(screenNVideo));
    }

    @Test
    void testDisplayFrameEdgeCases() throws Exception {
        // frame dispatch when no panel exists and when dropped
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);

        UIImage unknownFrame = new UIImage(img, "8.8.8.8", (byte)1);
        ScreenNVideo.displayFrame(unknownFrame);
        flushEDT();
        assertEquals((byte) 0, unknownFrame.isSuccess());

        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        flushEDT();

        List<UserProfile> users = new ArrayList<>();
        users.add(new UserProfile("u1", "U1", null, ParticipantRole.STUDENT));
        users.get(0).setIp("1.1.1.1");
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        UIImage validFrame = new UIImage(img, "1.1.1.1", (byte) 1);
        ScreenNVideo.displayFrame(validFrame);

        UIImage droppedFrame = new UIImage(img, "1.1.1.1", (byte) 1);
        ScreenNVideo.displayFrame(droppedFrame);
        assertEquals((byte) 0, droppedFrame.isSuccess());
        flushEDT();
    }

    @Test
    void testGhostPanelVisibility() throws Exception {
        // ghost panel should not appear in visible set
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        ScreenNVideoModel model = ScreenNVideoModel.getInstance(fakeRpc);
        JScrollPane scrollPane = getScrollPane(screenNVideo);

        Map<String, ParticipantPanel> panels = getParticipantPanels();
        if (panels != null) {
            ParticipantPanel ghost = new ParticipantPanel("Ghost", "0.0.0.0");
            panels.put("0.0.0.0", ghost);
        }

        resizeViewport(scrollPane, 500, 500);
        flushEDT();
        assertFalse(model.visibleParticipants.get().contains("0.0.0.0"));
    }

    @Test
    void testRemoveParticipantMissingPanel() throws Exception {
        // removing non-existent participant should not throw
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        flushEDT();

        Method removePart = ScreenNVideo.class.getDeclaredMethod("removeParticipant", String.class);
        removePart.setAccessible(true);
        assertDoesNotThrow(() -> removePart.invoke(screenNVideo, "999.999.999.999"));
    }

    @Test
    void testAddParticipantDuplicateDirectly() throws Exception {
        // adding same participant twice should be idempotent
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        flushEDT();

        Method addPart = ScreenNVideo.class.getDeclaredMethod("addParticipant", String.class, String.class);
        addPart.setAccessible(true);

        SwingUtilities.invokeAndWait(() -> {
            try { addPart.invoke(screenNVideo, "User1", "1.1.1.1"); } catch (Exception e) {}
        });

        Map<String, ParticipantPanel> panels = getParticipantPanels();
        assertEquals(1, panels.size());

        SwingUtilities.invokeAndWait(() -> {
            try { addPart.invoke(screenNVideo, "User1", "1.1.1.1"); } catch (Exception e) {}
        });
        assertEquals(1, panels.size());
    }

    @Test
    void testNonPanelComponentInGrid() throws Exception {
        // grid should tolerate non-panel components
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ScreenNVideo screenNVideo = new ScreenNVideo(meetingVM);
        flushEDT();

        List<UserProfile> users = new ArrayList<>();
        users.add(new UserProfile("u1", "U1", null, ParticipantRole.STUDENT));
        users.get(0).setIp("1.1.1.1");
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        JPanel videoGrid = getVideoGrid(screenNVideo);
        SwingUtilities.invokeAndWait(() -> videoGrid.add(new JLabel("Intruder")));

        JScrollPane scrollPane = getScrollPane(screenNVideo);
        resizeViewport(scrollPane, 800, 600);
        flushEDT();

        users.add(new UserProfile("u2", "U2", null, ParticipantRole.STUDENT));
        users.get(1).setIp("1.1.1.2");
        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(new ArrayList<>(users)));
        flushEDT();

        SwingUtilities.invokeAndWait(() -> screenNVideo.onZoomToggle("1.1.1.1"));
        flushEDT();
        assertTrue(videoGrid.getComponentCount() > 0);
    }
}
