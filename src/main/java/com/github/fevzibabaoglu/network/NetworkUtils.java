package com.github.fevzibabaoglu.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NetworkUtils {

    public static String getMACAddress(NetworkInterface networkInterface) throws SocketException {
        byte[] macAddress = networkInterface.getHardwareAddress();
        return macAddress == null ? null :
            IntStream.range(0, macAddress.length)
                .mapToObj(i -> String.format("%02X", macAddress[i]))
                .collect(Collectors.joining(":"));
    }

    // Find the PeerNetworkInterface of the subnet that the localIP is in
    public static PeerNetworkInterface ipMatch(Peer peer, InetAddress localIPAddress) {
        for (PeerNetworkInterface localPeerNetworkInterface : peer.getPeerNetworkInterfaces()) {
            if (localPeerNetworkInterface.getLocalIPAddress().getHostAddress().equals(localIPAddress.getHostAddress())) {
                return localPeerNetworkInterface;
            }
        }
        return null;
    }

    // Find the PeerNetworkInterface of the subnet that the targetIp is in
    public static PeerNetworkInterface subnetMatch(Peer peer, InetAddress targetIPAddress) throws UnknownHostException, SocketException {
        for (PeerNetworkInterface localPeerNetworkInterface : peer.getPeerNetworkInterfaces()) {
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
