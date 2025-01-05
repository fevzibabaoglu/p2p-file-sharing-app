package com.github.fevzibabaoglu.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.github.fevzibabaoglu.file.PeerFileMetadata;

public class Peer implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private final Set<String> macAddresses;
    private final Map<PeerNetworkInterface, Set<Peer>> interfacePeersMap;
    private Set<PeerFileMetadata> fileMetadatas;

    public Peer() throws SocketException {
        interfacePeersMap = new ConcurrentHashMap<>();
        macAddresses = new CopyOnWriteArraySet<>();
        fileMetadatas = null;

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            PeerNetworkInterface peerNetworkInterface = new PeerNetworkInterface(networkInterface);
            if (peerNetworkInterface.isUpIPv4Interface()) {
                interfacePeersMap.put(peerNetworkInterface, new CopyOnWriteArraySet<>());
                macAddresses.add(NetworkUtils.getMACAddress(networkInterface));
            }
        }
    }

    private Peer(Map<PeerNetworkInterface, Set<Peer>> interfacePeersMap, Set<String> macAddresses, Set<PeerFileMetadata> fileMetadatas) {
        this.interfacePeersMap = interfacePeersMap;
        this.macAddresses = macAddresses;
        this.fileMetadatas = fileMetadatas;
    }

    public Set<PeerFileMetadata> getFileMetadatas() {
        return fileMetadatas;
    }

    public void setFileMetadatas(Set<PeerFileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    public Set<PeerNetworkInterface> getPeerNetworkInterfaces() {
        return interfacePeersMap.keySet();
    }

    public Set<Peer> getKnownPeerList(PeerNetworkInterface peerNetworkInterface) {
        return interfacePeersMap.get(peerNetworkInterface);
    }

    public void addKnownPeerToInterface(PeerNetworkInterface peerNetworkInterface, Peer peer) {
        if (interfacePeersMap.containsKey(peerNetworkInterface) && !(interfacePeersMap.get(peerNetworkInterface).contains(peer))) {
            interfacePeersMap.get(peerNetworkInterface).add(peer);
        }
    }

    // Find the route from thisPeer to targetPeer
    public List<PeerNetworkInterface> getRouteToPeer(Peer targetPeer) throws SocketException, UnknownHostException {
        List<PeerNetworkInterface> route = new ArrayList<>();
        Set<Peer> visitedPeers = new HashSet<>();
        if (findRouteRecursive(this, targetPeer, route, visitedPeers)) {
            return route;
        }
        return null;
    }

    private static boolean findRouteRecursive(Peer currentPeer, Peer targetPeer, List<PeerNetworkInterface> route, Set<Peer> visitedPeers) throws SocketException, UnknownHostException {
        // Avoid revisiting peers
        if (!visitedPeers.add(currentPeer)) {
            return false;
        }

        // Check if the current peer is the target
        if (currentPeer.equals(targetPeer)) {
            return true;
        }

        // Process all known peers of the current peer
        for (Map.Entry<PeerNetworkInterface, Set<Peer>> entry : currentPeer.interfacePeersMap.entrySet()) {
            for (Peer knownPeer : entry.getValue()) {
                // Find the in-interface of the knownPeer that connects it to the currentPeer
                PeerNetworkInterface inInterface = NetworkUtils.subnetMatch(knownPeer, entry.getKey().getLocalIPAddress());
                if (inInterface != null) {
                    route.add(inInterface);
                    if (findRouteRecursive(knownPeer, targetPeer, route, visitedPeers)) {
                        return true;
                    }

                    // Backtrack
                    route.remove(route.size() - 1);
                }
            }
        }
        return false;
    }

    // DFS traversal
    // Assuming no circular references
    public List<Peer> getReachablePeers() {
        Set<Peer> visited = new HashSet<>();
        Stack<Peer> stack = new Stack<>();
        List<Peer> reachablePeers = new ArrayList<>();

        // Start DFS from the local peer
        stack.push(this);
        visited.add(this);

        while (!stack.isEmpty()) {
            Peer current = stack.pop();
            for (Set<Peer> neighbors : current.interfacePeersMap.values()) {
                for (Peer neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        reachablePeers.add(neighbor);
                        stack.push(neighbor);
                    }
                }
            }
        }

        return reachablePeers;
    }

    // Assuming no circular references
    public void mergePeer(Peer newPeer) {
        newPeer.interfacePeersMap.forEach((networkInterface, newPeers) -> {
            this.interfacePeersMap.merge(networkInterface, new CopyOnWriteArraySet<>(newPeers), (existingPeers, incomingPeers) -> {
                incomingPeers.forEach(incomingPeer -> 
                    existingPeers.stream()
                        .filter(existingPeer -> existingPeer.equals(incomingPeer))
                        .findFirst()
                        .ifPresentOrElse(
                            existingPeer -> existingPeer.mergePeer(incomingPeer),
                            () -> existingPeers.add(incomingPeer)
                        )
                );
                return existingPeers;
            });
        });
    }

    // Assuming no circular references
    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            return bos.toByteArray();
        }
    }

    public static Peer deserialize(byte[] data, int length) throws IOException, ClassNotFoundException {
        if (length < 0 || length > data.length) {
            throw new IllegalArgumentException("Invalid length: " + length);
        }
        byte[] truncatedData = new byte[length];
        System.arraycopy(data, 0, truncatedData, 0, length);
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(truncatedData);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Peer) ois.readObject();
        }
    }

    // Assuming no circular references
    @Override
    public Peer clone() {
        try {
            Map<PeerNetworkInterface, Set<Peer>> clonedMap = new ConcurrentHashMap<>();
            for (Map.Entry<PeerNetworkInterface, Set<Peer>> entry : interfacePeersMap.entrySet()) {
                PeerNetworkInterface key = entry.getKey().clone();
                Set<Peer> clonedSet = new CopyOnWriteArraySet<>(entry.getValue().stream().map(Peer::clone).toList());
                clonedMap.put(key, clonedSet);
            }
            Set<String> clonedMACs = new CopyOnWriteArraySet<>();
            for (String mac : macAddresses) {
                clonedMACs.add(mac);
            }
            Set<PeerFileMetadata> clonedFileMetadatas = new CopyOnWriteArraySet<>();
            for (PeerFileMetadata fileMetadata : fileMetadatas) {
                clonedFileMetadatas.add(fileMetadata.clone());
            }
            return new Peer(clonedMap, clonedMACs, clonedFileMetadatas);
        } catch (Exception e) {
            throw new AssertionError("Cloning Peer failed");
        }
    }

    // Assuming no circular references
    @Override
    public String toString() {
        return interfacePeersMap.toString();
    }

    // Assuming no circular references
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Peer other = (Peer) obj;
        return macAddresses.equals(other.macAddresses);
    }

    @Override
    public int hashCode() {
        return macAddresses.hashCode();
    }
}
