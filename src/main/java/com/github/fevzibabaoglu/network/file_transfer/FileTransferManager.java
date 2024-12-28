package com.github.fevzibabaoglu.network.file_transfer;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Set;

import com.github.fevzibabaoglu.file.FileManager;
import com.github.fevzibabaoglu.file.PeerFileMetadata;
import com.github.fevzibabaoglu.network.NetworkUtils;
import com.github.fevzibabaoglu.network.Peer;
import com.github.fevzibabaoglu.network.PeerNetworkInterface;

public class FileTransferManager {
    
    private static final int LISTENING_PORT = 8002;

    private final FileManager fileManager;
    private Peer localPeer;

    public FileTransferManager(FileManager fileManager) throws SocketException {
        this.fileManager = fileManager;
        localPeer = new Peer();
    }

    public void setLocalPeer(Peer localPeer) {
        this.localPeer = localPeer;
    }

    public Peer getLocalPeer() {
        return localPeer;
    }

    // Starts the listener to accept incoming connections
    public void listen() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(LISTENING_PORT)) {
            while (true) {
                Socket incomingSocket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        handleIncomingConnection(incomingSocket);
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            incomingSocket.close();
                        } catch (IOException e) {}
                    }
                }).start();
            } 
        }
    }

    // Handles an incoming connection by receiving chunks and processing them
    private void handleIncomingConnection(Socket incomingSocket) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(incomingSocket.getInputStream())) {
            InetAddress receiveIPAddress = incomingSocket.getInetAddress();
            PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(localPeer, receiveIPAddress);
            InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

            while (true) {
                try {
                    FileChunkMessage chunkMessage = (FileChunkMessage) objectInputStream.readObject();

                    if (chunkMessage.getReceiver().equals(localPeer)) {
                        System.out.printf("[%s] %s.%d received from %s successfully.\n", localIPAddress, chunkMessage.getFileMetadata(), chunkMessage.getChunkIndex(), chunkMessage.getSender().getPeerNetworkInterfaces());
                        fileManager.saveChunk(chunkMessage);
                    } else {
                        forwardChunk(chunkMessage);
                    }
                } catch (EOFException e) {
                    // End of file/stream
                    break;
                }
            }
        }
    }

    // Forwards a chunk to the next peer
    public void forwardChunk(FileChunkMessage chunkMessage) throws IOException {
        List<PeerNetworkInterface> route = localPeer.getRouteToPeer(chunkMessage.getReceiver());
        if (route == null) {
            return;
        }

        PeerNetworkInterface targetPeerNetworkInterface = route.get(0);
        InetAddress targetIPAddress = targetPeerNetworkInterface.getLocalIPAddress();

        PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(localPeer, targetIPAddress);
        InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

        try (Socket outputSocket = new Socket(targetIPAddress, LISTENING_PORT);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputSocket.getOutputStream())) {

            System.out.printf("[%s] Message of %s forwarded to %s successfully.\n", localIPAddress, chunkMessage.getSender().getPeerNetworkInterfaces(), chunkMessage.getReceiver().getPeerNetworkInterfaces());

            objectOutputStream.writeObject(chunkMessage);
            objectOutputStream.flush();
        }
    }

    // Sends a file to a target peer
    public void sendChunks(Peer receiver, PeerFileMetadata fileMetadata, Set<Integer> chunkIndices) throws IOException {
        List<PeerNetworkInterface> route = localPeer.getRouteToPeer(receiver);
        if (route == null) {
            return;
        }

        PeerNetworkInterface targetPeerNetworkInterface = route.get(0);
        InetAddress targetIPAddress = targetPeerNetworkInterface.getLocalIPAddress();

        PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(localPeer, targetIPAddress);
        InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

        try (Socket outputSocket = new Socket(targetIPAddress, LISTENING_PORT);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputSocket.getOutputStream())) {

            for (int chunkIndex : chunkIndices) {
                byte[] chunkData = fileManager.getChunk(fileMetadata, chunkIndex);
                FileChunkMessage chunkMessage = new FileChunkMessage(localPeer, receiver, fileMetadata, chunkIndex, chunkData);
                
                System.out.printf("[%s] %s.%d sent to %s successfully.\n", localIPAddress, fileMetadata, chunkIndex, receiver.getPeerNetworkInterfaces());

                objectOutputStream.writeObject(chunkMessage);
                objectOutputStream.flush();
            }
        }
    }
}
