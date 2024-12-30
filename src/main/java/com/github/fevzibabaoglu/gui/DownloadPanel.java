package com.github.fevzibabaoglu.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.github.fevzibabaoglu.App;
import com.github.fevzibabaoglu.file.PeerFileMetadata;
import com.github.fevzibabaoglu.network.Peer;

public class DownloadPanel extends JPanel {

    private final App app;

    private final JTree peerTree;

    public DownloadPanel(App app) {
        this.app = app;
        peerTree = new JTree(new DefaultTreeModel(new DefaultMutableTreeNode("Peers")));
        configurePeerTree();

        // Found files
        JPanel foundPanel = new JPanel(new BorderLayout());
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found files"));
        foundPanel.add(new JScrollPane(peerTree), BorderLayout.CENTER);

        // Downloading files
        JPanel downloadingPanel = new JPanel(new BorderLayout());
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading files"));
        downloadingPanel.add(new JScrollPane(new JTable()), BorderLayout.CENTER);

        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        panel.add(foundPanel);
        panel.add(downloadingPanel);

        setLayout(new BorderLayout(10, 10));
        add(panel, BorderLayout.CENTER);
    }

    private void configurePeerTree() {
        // Set custom renderer
        peerTree.setCellRenderer(new CustomTreeCellRenderer());

        // Add mouse listener for right-click
        peerTree.addMouseListener(new MouseAdapter() {
            private void checkForPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    TreePath path = peerTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (!node.isRoot() && node.getLevel() != 1 && node.isLeaf()) {
                            showContextMenu(e, node);
                        }
                    }
                }
            }
        
            @Override
            public void mousePressed(MouseEvent e) {
                checkForPopup(e);
            }
        
            @Override
            public void mouseReleased(MouseEvent e) {
                checkForPopup(e);
            }
        });
    }

    private void showContextMenu(MouseEvent e, DefaultMutableTreeNode node) {
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem downloadItem = new JMenuItem("Download");

        downloadItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleDownloadAction(node);
            }
        });

        contextMenu.add(downloadItem);
        contextMenu.show(peerTree, e.getX(), e.getY());
    }

    private void handleDownloadAction(DefaultMutableTreeNode node) {
        PeerFileMetadata fileMetadata = (PeerFileMetadata) node.getUserObject();
        // TODO Add file download logic
    }

    public void updatePeerFileTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) peerTree.getModel().getRoot();
        root.removeAllChildren();

        Peer localPeer = app.getLocalPeer();
        if (localPeer != null) {
            for (Peer peer : localPeer.getReachablePeers()) {
                DefaultMutableTreeNode peerTree = new DefaultMutableTreeNode(peer.getPeerNetworkInterfaces());
                for (PeerFileMetadata file : peer.getFileMetadatas()) {
                    peerTree.add(new DefaultMutableTreeNode(file));
                }
                root.add(peerTree);
            }
        }

        // Notify the tree model about changes
        ((DefaultTreeModel) peerTree.getModel()).reload(root);
    }

    // Custom renderer to enforce folder icons for root and peer nodes
    static class CustomTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

            // Always use folder icon for root and peer nodes
            if (node.isRoot() || node.getLevel() == 1) {
                setIcon(getDefaultClosedIcon());
            } else if (leaf) {
                setIcon(getDefaultLeafIcon()); 
            }

            return this;
        }
    }
}
