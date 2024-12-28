package com.github.fevzibabaoglu.gui;

import javax.swing.*;
import java.awt.*;

public class FilesPanel extends JPanel {

    private JTextField sourceFolderField;
    private JTextField destinationFolderField;
    private DefaultListModel<String> exclusionListModel;

    public FilesPanel() {
        exclusionListModel = new DefaultListModel<>();

        // Shared folder
        sourceFolderField = new JTextField();
        JButton sourceBrowse = new JButton("Set");
        sourceBrowse.addActionListener(e -> sourceFolderField.setText(GUIUtils.getFolderFromDialog(this)));
        
        JPanel sourceFolderPanel = new JPanel(new BorderLayout(10, 10));
        sourceFolderPanel.setBorder(BorderFactory.createTitledBorder("Root of the P2P shared folder"));
        sourceFolderPanel.add(sourceFolderField, BorderLayout.CENTER);
        sourceFolderPanel.add(sourceBrowse, BorderLayout.EAST);

        // Destination folder
        destinationFolderField = new JTextField();
        JButton destinationBrowse = new JButton("Set");
        destinationBrowse.addActionListener(e -> destinationFolderField.setText(GUIUtils.getFolderFromDialog(this)));

        JPanel destinationFolderPanel = new JPanel(new BorderLayout(10, 10));
        destinationFolderPanel.setBorder(BorderFactory.createTitledBorder("Destination folder"));
        destinationFolderPanel.add(destinationFolderField, BorderLayout.CENTER);
        destinationFolderPanel.add(destinationBrowse, BorderLayout.EAST);
        
        // Main settings
        JPanel settingsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        settingsPanel.add(sourceFolderPanel);
        settingsPanel.add(destinationFolderPanel);
        
        // Exclusion settings
        JPanel exclusionPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        exclusionPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        exclusionPanel.add(createExclusionSection());
        exclusionPanel.add(createMaskSection());
        
        // FilesPanel layout
        setLayout(new BorderLayout(5, 5));
        add(settingsPanel, BorderLayout.NORTH);
        add(exclusionPanel, BorderLayout.CENTER);
    }

    private JPanel createExclusionSection() {
        JList<String> exclusionList = new JList<>(exclusionListModel);
        JScrollPane scrollPane = new JScrollPane(exclusionList);
        scrollPane.setPreferredSize(new Dimension(0, 50));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            exclusionListModel.addElement(GUIUtils.getFolderFromDialog(this));
        });

        JButton delButton = new JButton("Del");
        delButton.addActionListener(e -> {
            int selectedIndex = exclusionList.getSelectedIndex();
            if (selectedIndex != -1) {
                exclusionListModel.remove(selectedIndex);
            }
        });
        
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        buttonsPanel.add(addButton);
        buttonsPanel.add(delButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Exclude files under these folders"));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createMaskSection() {
        JTextArea maskText = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(maskText);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Exclude files matching these masks"));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }
}
