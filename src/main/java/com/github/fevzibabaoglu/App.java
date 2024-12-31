package com.github.fevzibabaoglu;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.github.fevzibabaoglu.file.FileManager;
import com.github.fevzibabaoglu.gui.MainFrame;
import com.github.fevzibabaoglu.network.Peer;
import com.github.fevzibabaoglu.network.broadcast.BroadcastManager;
import com.github.fevzibabaoglu.network.file_transfer.FileTransferManager;

public class App {

    public static final int CHUNK_SIZE = 256 * 1024;
    private static final int TTL = 3;
    private static final int BROADCAST_INTERVAL = 20000;

    private final ExecutorService threadPool;
    private final FileManager fileManager;
    private final BroadcastManager broadcastManager;
    private final FileTransferManager fileTransferManager;

    private final AtomicReference<Peer> localPeerRef;
    private final AtomicReference<MainFrame> mainFrameRef;
    
    private String sourcePath;
    private String destinationPath;
    
    public App(String defaultSourcePath, String defaultDestinationPath) throws IOException {
        sourcePath = defaultSourcePath;
        destinationPath = defaultDestinationPath;

        threadPool = Executors.newFixedThreadPool(4);
        fileManager = new FileManager(this.sourcePath, this.destinationPath, CHUNK_SIZE);
        broadcastManager = new BroadcastManager(fileManager, TTL);
        fileTransferManager = new FileTransferManager(fileManager);

        localPeerRef = new AtomicReference<>();
        mainFrameRef = new AtomicReference<>();
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
        fileManager.setPaths(sourcePath, destinationPath);
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
        fileManager.setPaths(sourcePath, destinationPath);
    }

    public Peer getLocalPeer() {
        return localPeerRef.get();
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public FileTransferManager getFileTransferManager() {
        return fileTransferManager;
    }

    public void initializeThreads() {
        threadPool.submit(() -> {
            try {
                while (true) {
                    broadcastManager.clearPeerCache();
                    broadcastManager.sendBroadcasts(null);
                    Thread.sleep(BROADCAST_INTERVAL);
                    localPeerRef.set(broadcastManager.getLocalPeer());
                    fileTransferManager.setLocalPeer(localPeerRef.get());
                    if (mainFrameRef.get() != null) {
                        mainFrameRef.get().getDownloadPanel().updatePeerFileTree();
                    }
                }
            } catch (InterruptedException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        threadPool.submit(() -> {
            try {
                broadcastManager.listenBroadcasts();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        threadPool.submit(() -> {
            try {
                broadcastManager.listenResponses();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        threadPool.submit(() -> {
            try {
                fileTransferManager.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void shutdownThreads() {
        threadPool.shutdownNow();
    }

    public static void main(String[] args) {
        String display = System.getenv("DISPLAY");
        if ((args.length > 0 && args[0].equalsIgnoreCase("-docker")) && (display == null || display.isEmpty())) {
            runDockerApp();
        } else {
            runGUIApp();
        }
    }

    private static void runGUIApp() {
        System.out.println("Starting the GUI app...");

        try {
            App app = new App("/home", "/home");
            SwingUtilities.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame(app, app.getFileManager(), app.getFileTransferManager());
                app.mainFrameRef.set(mainFrame);
                app.mainFrameRef.get().setVisible(true);
            });
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }

    private static void runDockerApp() {
        System.out.println("Starting the non-GUI app...");
        
        App app = null;
        try {
            app = new App("/home", "/home");

            app.fileManager.createRandomFile("test1", 1024 * 1024 + 15, 5);
            app.fileManager.createRandomFile("test2", 123 * 1024, 5);
            app.fileManager.createRandomFile("test3", 756 * 1024, 5);
            app.fileManager.createRandomFile("test4", 11 * 1024 * 1024 + 153, 5);
            app.fileManager.createRandomFile("test5", 123 * 1024 * 1024 + 282, 5);

            Thread.sleep(5000);
            app.initializeThreads();   

            // Keep the main thread alive
            while (true) {
                Thread.sleep(60000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (app != null) {
                app.shutdownThreads();
            }
        }
    }
}
