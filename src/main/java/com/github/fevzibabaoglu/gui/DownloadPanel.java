package com.github.fevzibabaoglu.gui;

import javax.swing.*;
import java.awt.*;

public class DownloadPanel extends JPanel {

    public DownloadPanel() {
        // Found files
        JPanel foundPanel = new JPanel(new BorderLayout());
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found files"));
        foundPanel.add(new JScrollPane(new JTable()), BorderLayout.CENTER);

        // Downloading files
        JPanel downloadingPanel = new JPanel(new BorderLayout());
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));
        downloadingPanel.add(new JScrollPane(new JTable()), BorderLayout.CENTER);

        // Search
        JPanel searchPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(foundPanel);
        panel.add(downloadingPanel);
        
        setLayout(new BorderLayout(10, 10));
        add(panel, BorderLayout.CENTER);
        add(searchPanel, BorderLayout.SOUTH);
    }
}

