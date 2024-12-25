package com.github.fevzibabaoglu.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class Peer implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private final Map<PeerNetworkInterface, Set<Peer>> interfacePeersMap;

    public Peer() throws SocketException {
        interfacePeersMap = new ConcurrentHashMap<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            PeerNetworkInterface peerNetworkInterface = new PeerNetworkInterface(networkInterface);
            if (peerNetworkInterface.isUpIPv4Interface()) {
                interfacePeersMap.put(peerNetworkInterface, new CopyOnWriteArraySet<>());
            }
        }
    }

    private Peer(Map<PeerNetworkInterface, Set<Peer>> interfacePeersMap) {
        this.interfacePeersMap = interfacePeersMap;
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
            return new Peer(clonedMap);
        } catch (Exception e) {
            throw new AssertionError("Cloning Peer failed");
        }
    }

    // Assuming no circular references
    @Override
    public String toString() {
        return String.format("Peer:{%s}", 
            interfacePeersMap.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty()) // Exclude entries with empty sets
                .map(entry -> String.format("Interface:%s=[%s]",
                    entry.getKey().toString(),
                    entry.getValue().stream().map(Peer::toString).collect(Collectors.joining(","))
                ))
                .collect(Collectors.joining(","))
        );
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
        return interfacePeersMap.keySet().equals(other.interfacePeersMap.keySet());
    }

    @Override
    public int hashCode() {
        return interfacePeersMap.keySet().hashCode();
    }
}
