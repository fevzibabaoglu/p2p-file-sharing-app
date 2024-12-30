package com.github.fevzibabaoglu.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.github.fevzibabaoglu.App;
import com.github.fevzibabaoglu.file.FileManager;
import com.github.fevzibabaoglu.file.PeerFileMetadata;
import com.github.fevzibabaoglu.network.Peer;
import com.github.fevzibabaoglu.network.PeerNetworkInterface;
import com.github.fevzibabaoglu.network.file_transfer.FileTransferManager;

public class DownloadPanel extends JPanel {

    private final App app;
    private final FileManager fileManager;
    private final FileTransferManager fileTransferManager;

    private final JTree peerTree;

    public DownloadPanel(App app, FileManager fileManager, FileTransferManager fileTransferManager) {
        this.app = app;
        this.fileManager = fileManager;
        this.fileTransferManager = fileTransferManager;
        
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
                try {
                    handleDownloadAction(node);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        contextMenu.add(downloadItem);
        contextMenu.show(peerTree, e.getX(), e.getY());
    }

    private void handleDownloadAction(DefaultMutableTreeNode node) throws IOException {
        PeerFileMetadata requestedFileMetadata = (PeerFileMetadata) node.getUserObject();
        List<Peer> peersPossessingFile = new ArrayList<>();
        List<PeerFileMetadata> fileMetadatas = new ArrayList<>();
        List<String> chunkFilenames = new ArrayList<>();

        for (Peer peer : app.getLocalPeer().getReachablePeers()) {
            for (PeerFileMetadata fileMetadata : peer.getFileMetadatas()) {
                if (fileMetadata.equals(requestedFileMetadata)) {
                    peersPossessingFile.add(peer);
                    fileMetadatas.add(fileMetadata);
                    break;
                }
            }
        }

        int numChunks = (int) Math.ceil((double) requestedFileMetadata.getFileSize() / App.CHUNK_SIZE);
        int numPeer = peersPossessingFile.size();
        int baseChunksPerPeer = numChunks / numPeer;
        int extraChunks = numChunks % numPeer;

        List<Integer> chunkIndices = IntStream.range(0, numChunks).boxed().collect(Collectors.toList());
        Collections.shuffle(chunkIndices);

        List<List<Integer>> chunksPerPeer = IntStream.range(0, numPeer)
            .mapToObj(i -> {
                int startIndex = i * baseChunksPerPeer + Math.min(i, extraChunks);
                int endIndex = startIndex + baseChunksPerPeer + (i < extraChunks ? 1 : 0);
                return chunkIndices.subList(startIndex, endIndex);
            })
            .collect(Collectors.toList());

        for (int i = 0; i < numPeer; i++) {
            Peer peer = peersPossessingFile.get(i);
            PeerFileMetadata fileMetadata = fileMetadatas.get(i);
            List<Integer> chunks = chunksPerPeer.get(i);
            if (!chunks.isEmpty()) {
                fileTransferManager.requestChunks(peer, fileMetadata, new HashSet<>(chunks));
                chunkFilenames.addAll(chunks.stream().map(chunk -> String.format("%s.%s", fileMetadata.getFilename(), chunk)).toList());
            }
        }

        new Thread(() -> {
            try {
                String outputFilename = requestedFileMetadata.getFilename();
                fileManager.mergeChunks(chunkFilenames, outputFilename);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void updatePeerFileTree() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) peerTree.getModel().getRoot();
        root.removeAllChildren();

        Peer localPeer = app.getLocalPeer();
        if (localPeer != null) {
            for (Peer peer : localPeer.getReachablePeers()) {
                DefaultMutableTreeNode peerTree = new DefaultMutableTreeNode(peer);
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

            // Change peer text to show peer network interfaces
            Object userObject = node.getUserObject();
            if (userObject instanceof Peer) {
                Peer peer = (Peer) userObject;
                setText(peer.getPeerNetworkInterfaces().stream().map(PeerNetworkInterface::toString).toList().toString());
            }

            return this;
        }
    }
}
