/**
 *  Contributed by Sandeep Kumar.
 */

package com.swe.ux.ui;

import com.swe.ux.theme.Theme;
import com.swe.ux.theme.ThemeManager;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * UI component representing a single participant in the video grid.
 * Displays the user's video feed or their initials if no video is available.
 */
public class ParticipantPanel extends JPanel {

    /**
     * A listener interface for handling zoom toggle requests from this panel.
     */
    public interface ParticipantPanelListener {
        void onZoomToggle(String ip);
    }

    /**
     * The display name of the participant.
     */
    private final String name;

    /**
     * The IP address of the participant.
     */
    private final String ip;

    /**
     * The current video frame being displayed.
     */
    private BufferedImage displayImage;

    /**
     * Listener for handling panel events.
     */
    private ParticipantPanelListener listener;

    /**
     * Flag indicating if the mouse is currently hovering over the panel.
     */
    private boolean isMouseOver = false;

    /**
     * Flag indicating if this panel is currently zoomed in.
     */
    private boolean isZoomed = false;

    /**
     * The hit box for the zoom icon/button.
     */
    private final Rectangle zoomIconBounds = new Rectangle();

    // UI Constants

    /**
     * Arc size for rounded corners.
     */
    private static final int CORNER_ARC = 20;

    /**
     * Standard 5px offset.
     */
    private static final int OFFSET_5 = 5;

    /**
     * Standard 10px offset.
     */
    private static final int OFFSET_10 = 10;

    /**
     * Divisor for circle calculation.
     */
    private static final int DIV_3 = 3;

    /**
     * Standard 20px offset.
     */
    private static final int OFFSET_20 = 20;

    /**
     * Height of the overlay bar.
     */
    private static final int OVERLAY_HEIGHT = 30;

    /**
     * Alpha transparency for the overlay.
     */
    private static final int OVERLAY_ALPHA = 128;

    /**
     * Font size for the participant name.
     */
    private static final int FONT_SIZE_NAME = 14;

    /**
     * Minimum font size.
     */
    private static final int FONT_SIZE_MIN = 12;

    /**
     * Creates a new ParticipantPanel.
     * @param nameArg The name of the participant.
     * @param ipArg The IP of the participant.
     */
    public ParticipantPanel(final String nameArg, final String ipArg) {
        this.name = nameArg;
        this.ip = ipArg;
        this.displayImage = null;
        setLayout(new BorderLayout());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                isMouseOver = true;
                repaint();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                isMouseOver = false;
                repaint();
            }

            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 1 && isMouseOver
                        && zoomIconBounds.contains(e.getPoint()) && listener != null
                ) {
                    listener.onZoomToggle(ip);
                }
            }
        });
    }

    /**
     * Sets the listener that will be notified of zoom toggle events.
     * @param listenerArg The listener to set.
     */
    public void setImage(final BufferedImage listenerArg) {
        this.displayImage = listenerArg;
        repaint();
    }

    /**
     * Sets the listener that will be notified of zoom toggle events.
     * @param listenerArg The listener to set.
     */
    public void setParticipantListener(final ParticipantPanelListener listenerArg) {
        this.listener = listenerArg;
    }

    /**
     * Sets the zoom state of this panel.
     * @param isZoomedArg true if this panel is currently in the main zoom view, false otherwise.
     */
    public void setZoomed(final boolean isZoomedArg) {
        this.isZoomed = isZoomedArg;
        if (isMouseOver) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final Theme theme = ThemeManager.getInstance().getCurrentTheme();

        if (displayImage != null) {
            g2d.drawImage(displayImage, 0, 0, getWidth(), getHeight(), null);
        } else {
            // Default participant view
            g2d.setColor(theme.getForeground());
            g2d.fillRoundRect(
                    OFFSET_5, OFFSET_5, getWidth() - OFFSET_10,
                    getHeight() - OFFSET_10, CORNER_ARC, CORNER_ARC
            );

            final int circleDiameter = Math.min(getWidth(), getHeight()) / 3;
            final int circleX = (getWidth() - circleDiameter) / 2;
            final int circleY = (getHeight() - circleDiameter) / 2 - 10;
            g2d.setColor(theme.getBackground());
            g2d.fillOval(circleX, circleY, circleDiameter, circleDiameter);

            g2d.setColor(theme.getText());
            g2d.setFont(new Font("SansSerif", Font.BOLD, Math.max(FONT_SIZE_MIN, circleDiameter / DIV_3)));
            final String initials;
            if (name.contains(" ")) {
                initials = ("" + name.charAt(0) + name.substring(name.indexOf(" ") + 1).charAt(0)).toUpperCase();
            } else {
                initials = ("" + name.charAt(0)).toUpperCase();
            }
            g2d.drawString(initials,
                    circleX + (circleDiameter - g2d.getFontMetrics().stringWidth(initials)) / 2,
                    circleY + (circleDiameter - g2d.getFontMetrics().getHeight()) / 2 + g2d.getFontMetrics().getAscent()
            );

            g2d.setFont(new Font("SansSerif", Font.PLAIN, FONT_SIZE_NAME));
            g2d.drawString(name,
                    (getWidth() - g2d.getFontMetrics().stringWidth(name)) / 2,
                    circleY + circleDiameter + OFFSET_20);
        }

        // Draw overlay on hover
        if (isMouseOver) {
            final int barHeight = OVERLAY_HEIGHT;
            g2d.setColor(new Color(0, 0, 0, OVERLAY_ALPHA));
            g2d.fillRect(0, getHeight() - barHeight, getWidth(), barHeight);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, FONT_SIZE_NAME));

            // Draw zoom TEXT on the right
            final String zoomText;
            if (isZoomed) {
                zoomText = "[-]";
            } else {
                zoomText = "[+]";
            }
            final FontMetrics metrics = g2d.getFontMetrics();
            final int textWidth = metrics.stringWidth(zoomText);
            final int textX = getWidth() - textWidth - 10;
            final int textY = getHeight() - (barHeight / 2) + (metrics.getAscent() - metrics.getDescent()) / 2;

            g2d.drawString(zoomText, textX, textY);

            // Update the clickable bounds
            zoomIconBounds.setBounds(textX - OFFSET_5, getHeight() - barHeight, textWidth + OFFSET_10, barHeight);

            if (displayImage != null) {
                g2d.drawString(name, OFFSET_10, getHeight() - OFFSET_10);
            }
        }

        g2d.dispose();
    }
}
