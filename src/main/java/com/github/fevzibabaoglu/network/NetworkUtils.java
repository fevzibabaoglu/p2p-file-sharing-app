package com.github.fevzibabaoglu.network;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkUtils {

    // Find the PeerNetworkInterface of the subnet that the localIP is in
    public static PeerNetworkInterface IPMatch(Peer peer, InetAddress localIPAddress) {
        for (PeerNetworkInterface localPeerNetworkInterface : peer.getPeerNetworkInterfaces()) {
            // Check if the local IP is in
            if (localPeerNetworkInterface.getLocalIPAddress().getHostAddress().equals(localIPAddress.getHostAddress())) {
                return localPeerNetworkInterface;
            }
        }
        return null;
    }

    // Find the PeerNetworkInterface of the subnet that the targetIp is in
    public static PeerNetworkInterface SubnetMatch(Peer peer, InetAddress targetIPAddress) throws UnknownHostException, SocketException {
        for (PeerNetworkInterface localPeerNetworkInterface : peer.getPeerNetworkInterfaces()) {
            // Check if the target IP is within the same subnet
            if (isInSameSubnet(localPeerNetworkInterface, targetIPAddress)) {
                return localPeerNetworkInterface;
            }
        }
        return null;
    }

    private static boolean isInSameSubnet(PeerNetworkInterface localPeerNetworkInterface, InetAddress targetIPAddress) {
        InetAddress localIPAddress = localPeerNetworkInterface.getLocalIPAddress();

        // Get the subnet mask
        int prefixLength = localPeerNetworkInterface.getMaskLength();
        int subnetMask = 0xFFFFFFFF << (32 - prefixLength);

        // Convert IP addresses to integers
        int targetIpInt = ipToInt(targetIPAddress.getHostAddress());
        int localIpInt = ipToInt(localIPAddress.getHostAddress());

        // Check if the target IP is within the same subnet
        return ((targetIpInt & subnetMask) == (localIpInt & subnetMask));
    }

    private static int ipToInt(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (Integer.parseInt(octets[i]) & 0xFF) << (24 - (i * 8));
        }
        return result;
    }
}
