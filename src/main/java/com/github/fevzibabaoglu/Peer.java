package com.github.fevzibabaoglu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class Peer implements Serializable {

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

    public PeerNetworkInterface getPeerNetworkInterface(String IPv4Address) {
        for (PeerNetworkInterface peerNetworkInterface : getPeerNetworkInterfaces()) {
            if (peerNetworkInterface.getLocalIPAddress().getHostAddress().equals(IPv4Address)) {
                return peerNetworkInterface;
            }
        }
        return null;
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

    public void addKnownPeersToInterface(PeerNetworkInterface peerNetworkInterface, List<Peer> peers) {
        if (interfacePeersMap.containsKey(peerNetworkInterface)) {
            for (Peer peer : peers) {
                addKnownPeerToInterface(peerNetworkInterface, peer);
            }
        }
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            return bos.toByteArray();
        }
    }

    public static Peer deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Peer) ois.readObject();
        }
    }

    @Override
    public String toString() {
        return String.format("{%s}", 
            interfacePeersMap.entrySet().stream()
                .map(entry -> String.format("Interface:%s=[%s]",
                    entry.getKey().toString(),
                    entry.getValue().isEmpty()
                        ? ""
                        : entry.getValue().stream()
                            .map(peer -> peer == this ? "SELF" : peer.toString()) // Handle self-references
                            .collect(Collectors.joining(","))
                ))
                .collect(Collectors.joining(","))
        );
    }
}
