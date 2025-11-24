/**
 *  Contributed by Sandeep Kumar.
 */

package com.swe.ux.view;

import com.swe.controller.Meeting.UserProfile;
import com.swe.ux.binding.PropertyListeners;
import com.swe.ux.theme.Theme;
import com.swe.ux.theme.ThemeManager;
import com.swe.ux.viewmodel.ParticipantsViewModel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * View component for displaying meeting participants.
 * Shows participant count and list of participant names.
 */
public class ParticipantsView extends JPanel {
    /**
     * ViewModel containing the participant data.
     */
    private final ParticipantsViewModel viewModel;

    /**
     * Label to display the count of participants.
     */
    private JLabel countLabel;

    /**
     * Panel container for the list of participants.
     */
    private JPanel participantsListPanel;

    /**
     * ScrollPane for the participant list.
     */
    private JScrollPane scrollPane;

    // UI Constants

    /**
     * Standard gap size.
     */
    private static final int GAP = 10;

    /**
     * Standard padding size.
     */
    private static final int PADDING = 10;

    /**
     * Standard internal padding for participant item borders.
     */
    private static final int ITEM_BORDER_PADDING = 5;

    /**
     * Font size for the header label.
     */
    private static final int HEADER_FONT_SIZE = 16;

    /**
     * Font size for list items.
     */
    private static final int LIST_ITEM_FONT_SIZE = 14;

    /**
     * Height of a single participant item.
     */
    private static final int LIST_ITEM_HEIGHT = 50;

    /**
     * Size of the online status indicator.
     */
    private static final int ONLINE_INDICATOR_SIZE = 10;

    /**
     * Spacing between list items.
     */
    private static final int LIST_ITEM_SPACING = 8;

    // Colors

    /**
     * Red component for Green color.
     */
    private static final int GREEN_R = 76;

    /**
     * Green component for Green color.
     */
    private static final int GREEN_G = 175;

    /**
     * Blue component for Green color.
     */
    private static final int GREEN_B = 80;
    
    /**
     * Creates a new ParticipantsView.
     * @param viewModelArg The ParticipantsViewModel to use
     */
    public ParticipantsView(final ParticipantsViewModel viewModelArg) {
        this.viewModel = viewModelArg;
        initializeUI();
        setupBindings();
        applyTheme();
    }
    
    /**
     * Initializes the UI components.
     */
    private void initializeUI() {
        setLayout(new BorderLayout(GAP, GAP));
        setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        
        // Header with participant count
        final JPanel headerPanel = new JPanel(new BorderLayout());
        countLabel = new JLabel("Participants (0)", JLabel.LEFT);
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, HEADER_FONT_SIZE));
        headerPanel.add(countLabel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);
        
        // Participants list
        participantsListPanel = new JPanel();
        participantsListPanel.setLayout(new BoxLayout(participantsListPanel, BoxLayout.Y_AXIS));
        participantsListPanel.setBorder(new EmptyBorder(PADDING, 0, 0, 0));
        
        scrollPane = new JScrollPane(participantsListPanel);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Sets up bindings between ViewModel and UI components.
     */
    private void setupBindings() {
        // Listen to participant count changes
        viewModel.participantCount.addListener(evt -> {
            SwingUtilities.invokeLater(() -> {
                final int count = viewModel.participantCount.get();
                countLabel.setText("Participants (" + count + ")");
            });
        });
        
        // Listen to participant list changes
        viewModel.participants.addListener(PropertyListeners.onListChanged((List<UserProfile> participants) -> {
            SwingUtilities.invokeLater(() -> {
                updateParticipantsList(participants);
            });
        }));
        
        // Initial update
        SwingUtilities.invokeLater(() -> {
            final int count = viewModel.participantCount.get();
            countLabel.setText("Participants (" + count + ")");
            updateParticipantsList(viewModel.getParticipants());
        });
    }
    
    /**
     * Updates the participants list display.
     * @param participants The list of participants to display
     */
    private void updateParticipantsList(final List<UserProfile> participants) {
        participantsListPanel.removeAll();
        
        if (participants == null || participants.isEmpty()) {
            final JLabel emptyLabel = new JLabel("No participants", JLabel.CENTER);
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, LIST_ITEM_FONT_SIZE));
            emptyLabel.setForeground(Color.GRAY);
            participantsListPanel.add(emptyLabel);
        } else {
            for (UserProfile participant : participants) {
                final JPanel participantItem = createParticipantItem(participant);
                participantsListPanel.add(participantItem);
                participantsListPanel.add(Box.createVerticalStrut(LIST_ITEM_SPACING));
            }
        }
        
        participantsListPanel.revalidate();
        participantsListPanel.repaint();
    }
    
    /**
     * Creates a panel for a single participant item.
     * @param participant The participant user
     * @return A JPanel displaying the participant information
     */
    private JPanel createParticipantItem(final UserProfile participant) {
        final JPanel itemPanel = new JPanel(new BorderLayout(GAP, 5));
        itemPanel.setBorder(new EmptyBorder(
                ITEM_BORDER_PADDING, ITEM_BORDER_PADDING, ITEM_BORDER_PADDING, ITEM_BORDER_PADDING
        ));
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, LIST_ITEM_HEIGHT));
        
        // Participant name
        final String displayName = participant.getDisplayName();
        final String name;
        if (displayName != null && !displayName.isEmpty()) {
            name = displayName;
        } else {
            name = participant.getEmail();
        }
        final JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, LIST_ITEM_FONT_SIZE));
        itemPanel.add(nameLabel, BorderLayout.WEST);
        
        // Online indicator (small circle)
        final JPanel indicatorPanel = new JPanel();
        indicatorPanel.setPreferredSize(new Dimension(ONLINE_INDICATOR_SIZE, ONLINE_INDICATOR_SIZE));
        indicatorPanel.setOpaque(true);
        indicatorPanel.setBackground(new Color(GREEN_R, GREEN_G, GREEN_B)); // Green for online
        itemPanel.add(indicatorPanel, BorderLayout.EAST);
        
        return itemPanel;
    }
    
    /**
     * Applies the current theme to the component.
     */
    private void applyTheme() {
        final ThemeManager themeManager = ThemeManager.getInstance();
        final Theme theme = themeManager.getCurrentTheme();
        
        setBackground(theme.getBackgroundColor());
        countLabel.setForeground(theme.getTextColor());
        participantsListPanel.setBackground(theme.getBackgroundColor());
        scrollPane.getViewport().setBackground(theme.getBackgroundColor());
        
        // Apply theme to all participant items
        for (Component comp : participantsListPanel.getComponents()) {
            if (comp instanceof JPanel itemPanel) {
                itemPanel.setBackground(theme.getBackgroundColor());
                for (Component child : itemPanel.getComponents()) {
                    if (child instanceof JLabel label) {
                        label.setForeground(theme.getTextColor());
                    }
                }
            }
        }
    }
}

