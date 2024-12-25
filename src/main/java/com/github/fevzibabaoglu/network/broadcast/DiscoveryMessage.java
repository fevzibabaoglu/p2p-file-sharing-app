package com.github.fevzibabaoglu.network.broadcast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.fevzibabaoglu.network.Peer;
import com.github.fevzibabaoglu.network.PeerNetworkInterface;

public class DiscoveryMessage implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private int ttl;
    private final Peer owner;
    private final List<PeerNetworkInterface> interfaceList;

    public DiscoveryMessage(int ttl, Peer owner) {
        this.ttl = ttl;
        this.owner = owner;
        this.interfaceList = new CopyOnWriteArrayList<>();
    }

    private DiscoveryMessage(int ttl, Peer owner, List<PeerNetworkInterface> interfaceList) {
        this.ttl = ttl;
        this.owner = owner;
        this.interfaceList = interfaceList;
    }

    public int getTtl() {
        return ttl;
    }
    
    public void decreaseTtl() {
        ttl -= 1;
    }

    public Peer getOwner() {
        return owner;
    }


    public Peer getRoutePeerByIndex(int index) {
        Peer routePeer = getOwner();
        int totalHops = interfaceList.size() / 2;

        if (index < 0) {
            index += (totalHops + 1);
        }

        if (index < 0 || index >= totalHops) {
            return null;
        }
    
        // Iterate through the route to find the peer at the specified index
        for (int i = 0; i <= index * 2; i += 2) {
            PeerNetworkInterface outInterface = interfaceList.get(i);
            PeerNetworkInterface inInterface = interfaceList.get(i + 1);

            for (Peer peer : routePeer.getKnownPeerList(outInterface)) {
                if (peer.getPeerNetworkInterfaces().contains(inInterface)) {
                    routePeer = peer;
                    break;
                }
            }
        }
        return routePeer;
    }    

    // Every inInterface is at odd indices: 1, 3, 5...
    public PeerNetworkInterface getInInterfaceByIndex(int index) {
        List<PeerNetworkInterface> inInterfaces = getInInterfaceList();
        if (index < 0) {
            index += inInterfaces.size();
        }
        if (index < 0 || index >= inInterfaces.size()) {
            return null;
        }
        return inInterfaces.get(index);
    }

    // Every inInterface is at odd indices: 1, 3, 5...
    private List<PeerNetworkInterface> getInInterfaceList() {
        List<PeerNetworkInterface> inInterfaces = new CopyOnWriteArrayList<>();
        for (int i = 1; i < interfaceList.size(); i += 2) {
            inInterfaces.add(interfaceList.get(i));
        }
        return inInterfaces;
    }
    
    // Every outInterface is at even indices: 0, 2, 4...
    public PeerNetworkInterface getOutInterfaceByIndex(int index) {
        List<PeerNetworkInterface> outInterfaces = getOutInterfaceList();
        if (index < 0) {
            index += outInterfaces.size();
        }
        if (index < 0 || index >= outInterfaces.size()) {
            return null;
        }
        return outInterfaces.get(index);
    }

    // Every outInterface is at even indices: 0, 2, 4...
    private List<PeerNetworkInterface> getOutInterfaceList() {
        List<PeerNetworkInterface> outInterfaces = new CopyOnWriteArrayList<>();
        for (int i = 0; i < interfaceList.size(); i += 2) {
            outInterfaces.add(interfaceList.get(i));
        }
        return outInterfaces;
    }
    
    public void addToInterfaceList(PeerNetworkInterface peerNetworkInterface) {
        interfaceList.add(peerNetworkInterface);
    }

    public void removeLastHopFromInterfaceList() {
        int size = interfaceList.size();

        // Ensure there are at least two elements to remove
        if (size >= 2) {
            interfaceList.remove(size - 1);
            interfaceList.remove(size - 2);
        }
    }

    public boolean isInterfaceListEmpty() {
        return interfaceList.isEmpty();
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            return bos.toByteArray();
        }
    }

    public static DiscoveryMessage deserialize(byte[] data, int length) throws IOException, ClassNotFoundException {
        if (length < 0 || length > data.length) {
            throw new IllegalArgumentException("Invalid length: " + length);
        }
        byte[] truncatedData = new byte[length];
        System.arraycopy(data, 0, truncatedData, 0, length);
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(truncatedData);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (DiscoveryMessage) ois.readObject();
        }
    }

    @Override
    public DiscoveryMessage clone() {
        try {
            return new DiscoveryMessage(
                this.ttl,
                this.owner.clone(),
                new CopyOnWriteArrayList<>(this.interfaceList.stream().map(PeerNetworkInterface::clone).toList())
            );
        } catch (Exception e) {
            throw new AssertionError("Cloning DiscoveryMessage failed");
        }
    }
}
