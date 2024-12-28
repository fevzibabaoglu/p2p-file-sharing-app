package com.github.fevzibabaoglu.network.broadcast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.github.fevzibabaoglu.file.FileManager;
import com.github.fevzibabaoglu.network.NetworkUtils;
import com.github.fevzibabaoglu.network.Peer;
import com.github.fevzibabaoglu.network.PeerNetworkInterface;

public class BroadcastManager {

    private static final int BROADCAST_PORT = 8000;
    private static final int RESPONSE_PORT = 8001;
    private static final int BUFFER_SIZE = 1024 * 1024;

    private final FileManager fileManager;
    private final int ttl;
    private Peer localPeer;
    private Peer tempLocalPeer;

    public BroadcastManager(FileManager fileManager, int ttl) throws IOException {
        this.fileManager = fileManager;
        this.ttl = ttl;
        clearPeerCache();
    }

    public void clearPeerCache() throws IOException {
        localPeer = new Peer();
        tempLocalPeer = new Peer();
        tempLocalPeer.setFileMetadatas(fileManager.listSharedFiles());
    }

    public Peer getLocalPeer() {
        return localPeer;
    }

    // Broadcast a discovery message on all network interfaces
    public void sendBroadcasts(DiscoveryMessage message) throws IOException {
        if (message == null) {
            message = new DiscoveryMessage(ttl, tempLocalPeer);
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            for (PeerNetworkInterface localPeerNetworkInterface : tempLocalPeer.getPeerNetworkInterfaces()) {
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
        try (DatagramSocket socket = new DatagramSocket(BROADCAST_PORT)) {
            socket.setBroadcast(true);

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);
                DiscoveryMessage receivedMessage = DiscoveryMessage.deserialize(packet.getData(), packet.getLength());

                InetAddress receiveIPAddress = packet.getAddress();
                PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(tempLocalPeer, receiveIPAddress);
                InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

                // If receiver is the sender
                if (receiveIPAddress.getHostAddress().equals(localIPAddress.getHostAddress())){
                    continue;
                }

                // Add the in interface to route list
                receivedMessage.addToInterfaceList(localPeerNetworkInterface);

                // Add the localPeer to previous peer's known peers (index -1 is localPeer, index -2 is previous peer)
                Peer previousPeer = receivedMessage.getRoutePeerByIndex(-2);
                previousPeer.addKnownPeerToInterface(receivedMessage.getOutInterfaceByIndex(-1), tempLocalPeer);

                System.out.printf("[%s] Broadcast of %s is captured\n", localIPAddress, receivedMessage.getOwner().getPeerNetworkInterfaces());

                // Forward broadcast
                receivedMessage.decreaseTtl();
                if (receivedMessage.getTtl() > 0) {
                    sendBroadcasts(receivedMessage);
                }

                // Send response
                sendResponse(receivedMessage);
            }
        }
    }

    // Listen for incoming responses to the discovery broadcast
    public void listenResponses() throws IOException, ClassNotFoundException {
        try (DatagramSocket socket = new DatagramSocket(RESPONSE_PORT)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(packet);
                DiscoveryMessage receivedMessage = DiscoveryMessage.deserialize(packet.getData(), packet.getLength());

                InetAddress receivedIPAddress = packet.getAddress();
                PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(tempLocalPeer, receivedIPAddress);
                InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

                // If receivedMessage owner is the localPeer, update the localPeer with new info, else forward response
                if (NetworkUtils.ipMatch(receivedMessage.getOwner(), localIPAddress) != null) {
                    localPeer.mergePeer(receivedMessage.getOwner());
                    System.out.printf("[%s] Discovered new peers\n", localIPAddress);
                } else {
                    sendResponse(receivedMessage);
                }

            }
        }
    }

    // Send a response for incoming discovery broadcast
    private void sendResponse(DiscoveryMessage message) throws IOException {
        // Find previous message broadcaster
        PeerNetworkInterface sendPeerNetworkInterface  = message.getOutInterfaceByIndex(-1);
        InetAddress sendIPAddress = sendPeerNetworkInterface.getLocalIPAddress();

        // Find local address
        PeerNetworkInterface localPeerNetworkInterface = NetworkUtils.subnetMatch(tempLocalPeer, sendIPAddress);
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
