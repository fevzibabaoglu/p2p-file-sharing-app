package com.github.fevzibabaoglu.gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.*;

import com.github.fevzibabaoglu.App;
import com.github.fevzibabaoglu.file.FileManager;
import com.github.fevzibabaoglu.network.file_transfer.FileTransferManager;

public class MainFrame extends JFrame {

    private final App app;
    private final FilesPanel filesPanel;
    private final DownloadPanel downloadPanel;

    public MainFrame(App app, FileManager fileManager, FileTransferManager fileTransferManager) {
        this.app = app;

        // Set up the main frame
        setTitle("P2P File Sharing Application");
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Customize close operation
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExit();
            }
        });

        // Create menu bar
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // Add panels
        filesPanel = new FilesPanel(fileManager);
        downloadPanel = new DownloadPanel(app, fileManager, fileTransferManager);

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
        exitItem.addActionListener(e -> handleExit());

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

    private void showAboutDialog() {
        String devInfo = "P2P File Sharing Application\nDeveloped by Fevzi Babaoglu (20210702020)";
        GUIUtils.showMessageDialog(this, devInfo, "About");
    }

    private void handleConnect() {
        app.initializeThreads();
    }

    private void handleDisconnect() {
        try {
            app.shutdownThreads();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleExit() {
        handleDisconnect();
        System.exit(0);
    }

    public FilesPanel getFilesPanel() {
        return filesPanel;
    }

    public DownloadPanel getDownloadPanel() {
        return downloadPanel;
    }
}
