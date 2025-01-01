package com.github.fevzibabaoglu.network.file_transfer;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
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

    // Starts the listener to accept incoming connections
    public void listen() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(LISTENING_PORT)) {
            while (true) {
                Socket incomingSocket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        handleIncomingConnection(incomingSocket);
                    } catch (ClassNotFoundException | IOException | NoSuchAlgorithmException e) {
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
    private void handleIncomingConnection(Socket incomingSocket) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(incomingSocket.getInputStream())) {
            InetAddress receiveIPAddress = incomingSocket.getInetAddress();
            PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(localPeer, receiveIPAddress);
            InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

            while (true) {
                try {
                    Object object = objectInputStream.readObject();
                    if (object instanceof FileChunkMessage) {
                        FileChunkMessage chunkMessage = (FileChunkMessage) object;
    
                        if (chunkMessage.getReceiver().equals(localPeer)) {
                            System.out.printf("[%s] %s.%d received from %s successfully.\n", localIPAddress, chunkMessage.getFileMetadata(), chunkMessage.getChunkIndex(), chunkMessage.getSender().getPeerNetworkInterfaces());
                            fileManager.saveChunk(chunkMessage);
                        } else {
                            forwardMessage(chunkMessage);
                        }
                    } else if (object instanceof PeerFileMetadataRequestMessage) {
                        PeerFileMetadataRequestMessage metadataRequestMessage = (PeerFileMetadataRequestMessage) object;

                        if (metadataRequestMessage.getReceiver().equals(localPeer)) { 
                            System.out.printf("[%s] File request received from %s successfully.\n", localIPAddress, metadataRequestMessage.getSender().getPeerNetworkInterfaces());
                            sendChunks(metadataRequestMessage.getSender(), metadataRequestMessage.getFileMetadata(), metadataRequestMessage.getChunkIndices());
                        } else {
                            forwardMessage(metadataRequestMessage);
                        }
                    }
                } catch (EOFException e) {
                    // End of file/stream
                    break;
                }
            }
        }
    }

    // Forwards a message to the next peer
    private void forwardMessage(Message message) throws IOException {
        List<PeerNetworkInterface> route = localPeer.getRouteToPeer(message.getReceiver());
        if (route == null) {
            return;
        }

        PeerNetworkInterface targetPeerNetworkInterface = route.get(0);
        InetAddress targetIPAddress = targetPeerNetworkInterface.getLocalIPAddress();

        PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(localPeer, targetIPAddress);
        InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

        try (Socket outputSocket = new Socket(targetIPAddress, LISTENING_PORT);
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputSocket.getOutputStream())) {

            System.out.printf("[%s] Message of %s forwarded to %s successfully.\n", localIPAddress, message.getSender().getPeerNetworkInterfaces(), message.getReceiver().getPeerNetworkInterfaces());

            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        }
    }

    // Sends a file to a target peer
    private void sendChunks(Peer receiver, PeerFileMetadata fileMetadata, Set<Integer> chunkIndices) throws IOException {
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
                byte[] chunkData = fileManager.loadChunk(fileMetadata, chunkIndex);
                FileChunkMessage chunkMessage = new FileChunkMessage(localPeer, receiver, fileMetadata, chunkIndex, chunkData);
                
                System.out.printf("[%s] %s.%d sent to %s successfully.\n", localIPAddress, fileMetadata, chunkIndex, receiver.getPeerNetworkInterfaces());

                objectOutputStream.writeObject(chunkMessage);
                objectOutputStream.flush();
            }
        }
    }

    public void requestChunks(Peer receiver, PeerFileMetadata fileMetadata, Set<Integer> chunkIndices) throws IOException {
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

            PeerFileMetadataRequestMessage metadataRequestMessage = new PeerFileMetadataRequestMessage(localPeer, receiver, fileMetadata, chunkIndices);
            System.out.printf("[%s] File request sent to %s successfully.\n", localIPAddress, receiver.getPeerNetworkInterfaces());

            objectOutputStream.writeObject(metadataRequestMessage);
            objectOutputStream.flush();
        }
    }
}
