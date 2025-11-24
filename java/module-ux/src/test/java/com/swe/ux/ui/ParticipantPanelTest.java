package com.swe.ux.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantPanelTest {

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testInstantiationAndSetters() {
        String name = "John Doe";
        String ip = "192.168.1.5";

        ParticipantPanel panel = new ParticipantPanel(name, ip);
        assertNotNull(panel);

        // basic setter checks
        assertDoesNotThrow(() -> panel.setZoomed(true));
        assertDoesNotThrow(() -> panel.setZoomed(false));

        // set listener
        panel.setParticipantListener(targetIp -> {});
    }

    @Test
    void testPaintingLogic() {
        // cover paint branches
        ParticipantPanel panel = new ParticipantPanel("Alice", "10.0.0.1");
        panel.setSize(200, 200);

        BufferedImage scratch = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scratch.createGraphics();

        // paint with no image
        assertDoesNotThrow(() -> panel.paint(g2d));

        // paint with image
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        panel.setImage(img);
        assertDoesNotThrow(() -> panel.paint(g2d));

        g2d.dispose();
    }

    @Test
    void testMouseInteractionAndPaintingOverlay() {
        String testIp = "192.168.1.99";
        ParticipantPanel panel = new ParticipantPanel("Bob", testIp);
        panel.setSize(200, 200);

        AtomicBoolean wasClicked = new AtomicBoolean(false);
        panel.setParticipantListener(ip -> {
            if (ip.equals(testIp)) wasClicked.set(true);
        });

        MouseListener adapter = panel.getMouseListeners()[0];
        BufferedImage scratch = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scratch.createGraphics();

        // hover enter
        MouseEvent enterEvent = new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, 10, 10, 0, false);
        adapter.mouseEntered(enterEvent);

        // paint to set bounds and overlay
        panel.paint(g2d);

        // hover with image
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        panel.setImage(img);
        panel.paint(g2d);

        // zoom toggle states
        panel.setZoomed(true);
        panel.paint(g2d);
        panel.setZoomed(false);
        panel.paint(g2d);

        // valid click inside zoom icon area
        MouseEvent hitClick = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 190, 190, 1, false);
        adapter.mouseClicked(hitClick);
        assertTrue(wasClicked.get(), "Listener SHOULD trigger for click inside zoom bounds");

        // miss click outside
        wasClicked.set(false);
        MouseEvent missClick = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 5, 5, 1, false);
        adapter.mouseClicked(missClick);
        assertFalse(wasClicked.get(), "Listener should NOT trigger for click outside bounds");

        // mouse exit
        MouseEvent exitEvent = new MouseEvent(panel, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 10, 10, 0, false);
        adapter.mouseExited(exitEvent);
        panel.paint(g2d);

        g2d.dispose();
    }

    @Test
    void testClickEventBranches() {
        ParticipantPanel panel = new ParticipantPanel("Tester", "1.1.1.1");
        panel.setSize(200, 200);
        MouseListener adapter = panel.getMouseListeners()[0];

        BufferedImage scratch = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scratch.createGraphics();

        AtomicBoolean wasClicked = new AtomicBoolean(false);

        // branch: click count != 1
        panel.setParticipantListener(ip -> wasClicked.set(true));
        adapter.mouseEntered(new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, 0, 0, 10, 10, 0, false));
        panel.paint(g2d);

        MouseEvent doubleClick = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 190, 190, 2, false);
        adapter.mouseClicked(doubleClick);
        assertFalse(wasClicked.get(), "Should not trigger on double click");

        // branch: not hovering
        adapter.mouseExited(new MouseEvent(panel, MouseEvent.MOUSE_EXITED, 0, 0, 10, 10, 0, false));
        MouseEvent clickNoHover = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 190, 190, 1, false);
        adapter.mouseClicked(clickNoHover);
        assertFalse(wasClicked.get(), "Should not trigger if mouse is not over panel");

        // branch: listener null
        panel.setParticipantListener(null);
        adapter.mouseEntered(new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, 0, 0, 10, 10, 0, false));
        panel.paint(g2d);

        MouseEvent validClickNoListener = new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, 0, 0, 190, 190, 1, false);
        assertDoesNotThrow(() -> adapter.mouseClicked(validClickNoListener));

        g2d.dispose();
    }

    @Test
    void testNameFormatting() {
        ParticipantPanel panel = new ParticipantPanel("John Von Doe", "0.0.0.0");
        panel.setSize(100, 100);
        BufferedImage scratch = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        assertDoesNotThrow(() -> panel.paint(scratch.createGraphics()));

        ParticipantPanel panel2 = new ParticipantPanel("Cher", "0.0.0.0");
        panel2.setSize(100, 100);
        assertDoesNotThrow(() -> panel2.paint(scratch.createGraphics()));
    }
}
