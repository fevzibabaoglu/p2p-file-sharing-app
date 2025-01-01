package com.github.fevzibabaoglu.gui;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

import com.github.fevzibabaoglu.App;
import com.github.fevzibabaoglu.file.FileManager;

public class FilesPanel extends JPanel {

    private final App app;
    private final FileManager fileManager;
    
    private JTextField sourceFolderField;
    private JTextField destinationFolderField;
    private DefaultListModel<String> exclusionListModel;
    private JTextArea maskText;

    public FilesPanel(App app, FileManager fileManager) {
        this.app = app;
        this.fileManager = fileManager;

        sourceFolderField = new JTextField(app.getSourcePath());
        destinationFolderField = new JTextField(app.getDestinationPath());
        exclusionListModel = new DefaultListModel<>();
        maskText = new JTextArea();

        // Main settings
        JPanel settingsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        settingsPanel.add(createSourceBrowseSection());
        settingsPanel.add(createDestinatioJPanel());
        
        // Exclusion settings
        JPanel exclusionPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        exclusionPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        exclusionPanel.add(createSourceExclusionSection());
        exclusionPanel.add(createDownloadExclusionSection());
        
        // FilesPanel layout
        setLayout(new BorderLayout(5, 5));
        add(settingsPanel, BorderLayout.NORTH);
        add(exclusionPanel, BorderLayout.CENTER);
    }

    private JPanel createSourceBrowseSection() {
        JButton sourceBrowse = new JButton("Set");
        sourceBrowse.addActionListener(e -> handleSourceBrowse());
        
        JPanel sourceFolderPanel = new JPanel(new BorderLayout(10, 10));
        sourceFolderPanel.setBorder(BorderFactory.createTitledBorder("Root of the P2P shared folder"));
        sourceFolderPanel.add(sourceFolderField, BorderLayout.CENTER);
        sourceFolderPanel.add(sourceBrowse, BorderLayout.EAST);
        return sourceFolderPanel;
    }

    private JPanel createDestinatioJPanel() {
        JButton destinationBrowse = new JButton("Set");
        destinationBrowse.addActionListener(e -> handleDestinationBrowse());

        JPanel destinationFolderPanel = new JPanel(new BorderLayout(10, 10));
        destinationFolderPanel.setBorder(BorderFactory.createTitledBorder("Destination folder"));
        destinationFolderPanel.add(destinationFolderField, BorderLayout.CENTER);
        destinationFolderPanel.add(destinationBrowse, BorderLayout.EAST);
        return destinationFolderPanel;
    }

    private JPanel createSourceExclusionSection() {
        JList<String> exclusionList = new JList<>(exclusionListModel);
        JScrollPane scrollPane = new JScrollPane(exclusionList);
        scrollPane.setPreferredSize(new Dimension(0, 50));

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            String selectedPath = GUIUtils.getFolderFromDialog(this, app.getSourcePath());
            if (selectedPath != null) {
                exclusionListModel.addElement(selectedPath);
                Path excludedPath = Paths.get(selectedPath);
                fileManager.addExcludedPath(excludedPath);
            }
        });

        JButton delButton = new JButton("Del");
        delButton.addActionListener(e -> {
            int selectedIndex = exclusionList.getSelectedIndex();
            if (selectedIndex != -1) {
                Path excludedPath = Paths.get(exclusionListModel.get(selectedIndex));
                fileManager.removeExcludedPath(excludedPath);
                exclusionListModel.remove(selectedIndex);
            }
        });
        
        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        buttonsPanel.add(addButton);
        buttonsPanel.add(delButton);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Exclude sharing files under these folders"));
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonsPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createDownloadExclusionSection() {
        JScrollPane scrollPane = new JScrollPane(maskText);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Exclude downloading files matching these masks"));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void handleSourceBrowse() {
        sourceFolderField.setText(GUIUtils.getFolderFromDialog(this, null));
        app.setSourcePath(sourceFolderField.getText());
    }

    private void handleDestinationBrowse() {
        destinationFolderField.setText(GUIUtils.getFolderFromDialog(this, null));
        app.setDestinationPath(destinationFolderField.getText());
    }

    public List<String> getDownloadingMaskList() {
        return Arrays.asList(maskText.getText().split("\n"));
    }
}
