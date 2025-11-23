package com.swe.ux.view;

import com.swe.controller.Meeting.ParticipantRole;
import com.swe.controller.Meeting.UserProfile;
import com.swe.controller.RPCinterface.AbstractRPC;
import com.swe.ux.viewmodel.MeetingViewModel;
import com.swe.ux.viewmodel.ParticipantsViewModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantsViewTest {

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    static class FakeRPC implements AbstractRPC {
        @Override
        public void subscribe(String methodName, Function<byte[], byte[]> method) { }

        @Override
        public Thread connect(int portNumber) { return null; }

        @Override
        public CompletableFuture<byte[]> call(String methodName, byte[] data) {
            return CompletableFuture.completedFuture(new byte[0]);
        }
    }

    // process current EDT events
    private void flushEDT() throws InterruptedException, InvocationTargetException {
        SwingUtilities.invokeAndWait(() -> {});
    }

    @Test
    void testParticipantNameFallbackLogic() throws InterruptedException, InvocationTargetException {
        // setup
        FakeRPC fakeRpc = new FakeRPC();
        UserProfile currentUser = new UserProfile("me@test.com", "Me", "url", ParticipantRole.STUDENT);
        MeetingViewModel meetingVM = new MeetingViewModel(currentUser, fakeRpc);
        ParticipantsViewModel participantsVM = new ParticipantsViewModel(meetingVM);

        ParticipantsView view = new ParticipantsView(participantsVM);
        assertNotNull(view);
        flushEDT();

        // access internals
        JScrollPane scrollPane = (JScrollPane) ((BorderLayout)view.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        JPanel listPanel = (JPanel) scrollPane.getViewport().getView();

        List<UserProfile> users = new ArrayList<>();

        // valid display name
        users.add(new UserProfile("a@test.com", "User A", null, ParticipantRole.STUDENT));
        // null display name -> fallback to email
        users.add(new UserProfile("b@test.com", null, null, ParticipantRole.STUDENT));
        // empty display name -> fallback to email
        users.add(new UserProfile("c@test.com", "", null, ParticipantRole.STUDENT));

        SwingUtilities.invokeAndWait(() -> meetingVM.participants.set(users));
        flushEDT();

        JPanel item1 = (JPanel) listPanel.getComponent(0);
        JLabel label1 = (JLabel) ((BorderLayout)item1.getLayout()).getLayoutComponent(BorderLayout.WEST);
        assertEquals("User A", label1.getText());

        JPanel item2 = (JPanel) listPanel.getComponent(2);
        JLabel label2 = (JLabel) ((BorderLayout)item2.getLayout()).getLayoutComponent(BorderLayout.WEST);
        assertEquals("b@test.com", label2.getText());

        JPanel item3 = (JPanel) listPanel.getComponent(4);
        JLabel label3 = (JLabel) ((BorderLayout)item3.getLayout()).getLayoutComponent(BorderLayout.WEST);
        assertEquals("c@test.com", label3.getText());
    }

    @Test
    void testEmptyAndNullListHandling() throws InterruptedException, InvocationTargetException {
        // setup
        FakeRPC fakeRpc = new FakeRPC();
        MeetingViewModel meetingVM = new MeetingViewModel(new UserProfile(), fakeRpc);
        ParticipantsViewModel participantsVM = new ParticipantsViewModel(meetingVM);
        ParticipantsView view = new ParticipantsView(participantsVM);
        flushEDT();

        JScrollPane scrollPane = (JScrollPane) ((BorderLayout)view.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        JPanel listPanel = (JPanel) scrollPane.getViewport().getView();

        // test null list on ViewModel
        SwingUtilities.invokeAndWait(() -> participantsVM.participants.set(null));
        flushEDT();

        JLabel labelNull = (JLabel) listPanel.getComponent(0);
        assertEquals("No participants", labelNull.getText());

        // test empty list
        SwingUtilities.invokeAndWait(() -> participantsVM.participants.set(new ArrayList<>()));
        flushEDT();

        JLabel labelEmpty = (JLabel) listPanel.getComponent(0);
        assertEquals("No participants", labelEmpty.getText());
    }

    @Test
    void testApplyThemeDeepCoverage() throws Exception {
        // setup with data
        FakeRPC fakeRpc = new FakeRPC();
        UserProfile currentUser = new UserProfile("me@test.com", "Me", "url", ParticipantRole.STUDENT);
        MeetingViewModel meetingVM = new MeetingViewModel(currentUser, fakeRpc);

        List<UserProfile> users = new ArrayList<>();
        users.add(new UserProfile("theme@test.com", "Theme User", null, ParticipantRole.STUDENT));
        meetingVM.participants.set(users);

        ParticipantsViewModel participantsVM = new ParticipantsViewModel(meetingVM);
        ParticipantsView view = new ParticipantsView(participantsVM);

        flushEDT();

        JScrollPane scrollPane = (JScrollPane) ((BorderLayout)view.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        JPanel listPanel = (JPanel) scrollPane.getViewport().getView();
        assertTrue(listPanel.getComponentCount() > 0, "List should be populated before running theme test");

        // call private method to exercise loops
        Method applyThemeMethod = ParticipantsView.class.getDeclaredMethod("applyTheme");
        applyThemeMethod.setAccessible(true);
        applyThemeMethod.invoke(view);
    }
}
