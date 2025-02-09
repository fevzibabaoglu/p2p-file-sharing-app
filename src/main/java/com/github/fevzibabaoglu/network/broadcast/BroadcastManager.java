package com.github.fevzibabaoglu.network.broadcast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import com.github.fevzibabaoglu.App;
import com.github.fevzibabaoglu.file.FileManager;
import com.github.fevzibabaoglu.network.NetworkUtils;
import com.github.fevzibabaoglu.network.Peer;
import com.github.fevzibabaoglu.network.PeerNetworkInterface;

public class BroadcastManager {

    private static final int BROADCAST_PORT = 8000;
    private static final int RESPONSE_PORT = 8001;
    private static final int BUFFER_SIZE = 1024 * 1024;

    private final App app;
    private final FileManager fileManager;
    private final int ttl;

    private final AtomicReference<Peer> localPeerRef;
    private final AtomicReference<Peer> tempLocalPeerRef;

    public BroadcastManager(App app, FileManager fileManager, int ttl) throws IOException {
        this.app = app;
        this.fileManager = fileManager;
        this.ttl = ttl;
        localPeerRef = new AtomicReference<>();
        tempLocalPeerRef = new AtomicReference<>();
        clearPeerCache();
    }

    public synchronized void clearPeerCache() throws IOException {
        localPeerRef.set(new Peer());
        tempLocalPeerRef.set(new Peer());
        tempLocalPeerRef.get().setFileMetadatas(fileManager.listSharedFiles());
    }

    public Peer getLocalPeer() {
        return localPeerRef.get();
    }

    // Broadcast a discovery message on all network interfaces
    public synchronized void sendBroadcasts(DiscoveryMessage message) throws IOException {
        if (message == null) {
            message = new DiscoveryMessage(ttl, tempLocalPeerRef.get());
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            for (PeerNetworkInterface localPeerNetworkInterface : tempLocalPeerRef.get().getPeerNetworkInterfaces()) {
                // Exclude the interface where the message came from
                if (localPeerNetworkInterface.equals(message.getInInterfaceByIndex(-1))) {
                    continue;
                }
                
                InetAddress broadcastIPAddress = localPeerNetworkInterface.getBroadcastIPAddress();
                InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

                // Copy the message, do not edit same message more than once
                DiscoveryMessage copyMessage = message.clone();
                
                // Add the out interface to route list
                copyMessage.addToInterfaceList(localPeerNetworkInterface);

                System.out.printf("[%s] %s broadcasted on %s\n", localIPAddress, copyMessage.getOwner().getPeerNetworkInterfaces(), broadcastIPAddress);

                // Send broadcast
                byte[] messageByte = copyMessage.serialize();
                DatagramPacket packet = new DatagramPacket(messageByte, messageByte.length, broadcastIPAddress, BROADCAST_PORT);
                socket.send(packet);
            }
        }
    }

    // Listen for incoming broadcast messages on all network interfaces
    public void listenBroadcasts() throws IOException, ClassNotFoundException {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(BROADCAST_PORT);
            socket.setBroadcast(true);
            socket.setSoTimeout(5000);

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (app.isThreadsRunning()) {
                try {
                    socket.receive(packet);
                    DiscoveryMessage receivedMessage = DiscoveryMessage.deserialize(packet.getData(), packet.getLength());
    
                    InetAddress receiveIPAddress = packet.getAddress();
                    PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(tempLocalPeerRef.get(), receiveIPAddress);
                    InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();
    
                    // If receiver is the sender
                    if (receiveIPAddress.getHostAddress().equals(localIPAddress.getHostAddress())){
                        continue;
                    }
    
                    // Add the in interface to route list
                    receivedMessage.addToInterfaceList(localPeerNetworkInterface);
    
                    // Add the localPeer to previous peer's known peers (index -1 is localPeer, index -2 is previous peer)
                    Peer previousPeer = receivedMessage.getRoutePeerByIndex(-2);
                    previousPeer.addKnownPeerToInterface(receivedMessage.getOutInterfaceByIndex(-1), tempLocalPeerRef.get());
    
                    System.out.printf("[%s] Broadcast of %s is captured\n", localIPAddress, receivedMessage.getOwner().getPeerNetworkInterfaces());
    
                    // Forward broadcast
                    receivedMessage.decreaseTtl();
                    if (receivedMessage.getTtl() > 0) {
                        sendBroadcasts(receivedMessage);
                    }
    
                    // Send response
                    sendResponse(receivedMessage);
                } catch (SocketTimeoutException e) {}
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    // Listen for incoming responses to the discovery broadcast
    public void listenResponses() throws IOException, ClassNotFoundException {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(RESPONSE_PORT);
            socket.setSoTimeout(5000);

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (app.isThreadsRunning()) {
                try {
                    socket.receive(packet);
                    DiscoveryMessage receivedMessage = DiscoveryMessage.deserialize(packet.getData(), packet.getLength());
    
                    InetAddress receivedIPAddress = packet.getAddress();
                    PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(tempLocalPeerRef.get(), receivedIPAddress);
                    InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();
    
                    // If receivedMessage owner is the localPeer, update the localPeer with new info, else forward response
                    if (NetworkUtils.ipMatch(receivedMessage.getOwner(), localIPAddress) != null) {
                        synchronized (this) {
                            getLocalPeer().mergePeer(receivedMessage.getOwner());
                        }
                        System.out.printf("[%s] Discovered new peers\n", localIPAddress);
                    } else {
                        sendResponse(receivedMessage);
                    }
                } catch (SocketTimeoutException e) {}
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    // Send a response for incoming discovery broadcast
    private void sendResponse(DiscoveryMessage message) throws IOException {
        // Find previous message broadcaster
        PeerNetworkInterface sendPeerNetworkInterface  = message.getOutInterfaceByIndex(-1);
        InetAddress sendIPAddress = sendPeerNetworkInterface.getLocalIPAddress();

        // Find local address
        PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(tempLocalPeerRef.get(), sendIPAddress);
        InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

        // Remove last hop
        message.removeLastHopFromInterfaceList();

        System.out.printf("[%s] Response for %s sent/forwarded to %s\n", localIPAddress, message.getOwner().getPeerNetworkInterfaces(), sendIPAddress);

        byte[] messageByte = message.serialize();
        DatagramPacket responsePacket = new DatagramPacket(messageByte, messageByte.length, sendIPAddress, RESPONSE_PORT);
        try (DatagramSocket responseSocket = new DatagramSocket()) {
            responseSocket.send(responsePacket);
        }
    }
}
