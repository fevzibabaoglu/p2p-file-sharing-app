package com.github.fevzibabaoglu.gui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {
        // Set up the main frame
        setTitle("P2P File Sharing Application");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create menu bar
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // Add panels
        FilesPanel filesPanel = new FilesPanel();
        DownloadPanel downloadPanel = new DownloadPanel();

        // Set layout
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(filesPanel, BorderLayout.NORTH);
        contentPanel.add(downloadPanel, BorderLayout.CENTER);
        add(contentPanel);
    }

    private JMenuBar createMenuBar() {

        // Files Menu
        JMenuItem connectItem = new JMenuItem("Connect");
        connectItem.addActionListener(e -> handleConnect());

        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        disconnectItem.addActionListener(e -> handleDisconnect());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        JMenu filesMenu = new JMenu("Files");
        filesMenu.add(connectItem);
        filesMenu.add(disconnectItem);
        filesMenu.addSeparator();
        filesMenu.add(exitItem);

        // Help Menu
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());

        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(aboutItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(filesMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void handleConnect() {
        GUIUtils.showMessageDialog(this, "Connecting to the network...", "Connect");
    }

    private void handleDisconnect() {
        GUIUtils.showMessageDialog(this, "Disconnecting from the network...", "Disconnect");
    }

    private void showAboutDialog() {
        GUIUtils.showMessageDialog(this, "P2P File Sharing Application\nDeveloped by Fevzi Babaoglu (20210702020)", "About");
    }
}
