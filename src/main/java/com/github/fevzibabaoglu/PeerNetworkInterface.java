package com.github.fevzibabaoglu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

public class PeerNetworkInterface implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private final transient NetworkInterface networkInterface;
    private final InetAddress broadcastIPAddress;
    private final InetAddress localIPAddress;

    public PeerNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        this.networkInterface = networkInterface;

        if (this.networkInterface.isLoopback() || !this.networkInterface.isUp()) {
            this.localIPAddress = null;
            this.broadcastIPAddress = null;
            return;
        }

        for (InterfaceAddress interfaceAddress : this.networkInterface.getInterfaceAddresses()) {
            InetAddress localIP = interfaceAddress.getAddress();
            if (!(localIP instanceof Inet4Address)) {
                continue;
            }

            InetAddress broadcastIP = interfaceAddress.getBroadcast();
            if (broadcastIP == null) {
                continue;
            }

            this.localIPAddress = localIP;
            this.broadcastIPAddress = broadcastIP;
            return;
        }

        this.localIPAddress = null;
        this.broadcastIPAddress = null;
    }  

    public boolean isUpIPv4Interface() {
        return ((broadcastIPAddress != null) && (localIPAddress != null));
    } 

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public InetAddress getBroadcastIPAddress() {
        return broadcastIPAddress;
    }

    public InetAddress getLocalIPAddress() {
        return localIPAddress;
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            return bos.toByteArray();
        }
    }

    public static PeerNetworkInterface deserialzie(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (PeerNetworkInterface) ois.readObject();
        }
    }

    @Override
    public String toString() {
        return localIPAddress.getHostAddress();
    }
}
