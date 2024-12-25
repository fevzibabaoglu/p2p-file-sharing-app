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
    private final short maskLength;
    private final InetAddress broadcastIPAddress;
    private final InetAddress localIPAddress;

    public PeerNetworkInterface(NetworkInterface networkInterface) throws SocketException {
        this.networkInterface = networkInterface;

        if (this.networkInterface.isLoopback() || !this.networkInterface.isUp()) {
            this.maskLength = 0;
            this.localIPAddress = null;
            this.broadcastIPAddress = null;
            return;
        }

        for (InterfaceAddress interfaceAddress : this.networkInterface.getInterfaceAddresses()) {
            InetAddress localIPAddress = interfaceAddress.getAddress();
            if (!(localIPAddress instanceof Inet4Address)) {
                continue;
            }

            InetAddress broadcastIPAddress = interfaceAddress.getBroadcast();
            if (broadcastIPAddress == null) {
                continue;
            }

            this.maskLength = interfaceAddress.getNetworkPrefixLength();
            this.localIPAddress = localIPAddress;
            this.broadcastIPAddress = broadcastIPAddress;
            return;
        }

        this.maskLength = 0;
        this.localIPAddress = null;
        this.broadcastIPAddress = null;
    }  

    public boolean isUpIPv4Interface() {
        return ((broadcastIPAddress != null) && (localIPAddress != null));
    } 

    public NetworkInterface getNetworkInterface() {
        return networkInterface;
    }

    public short getMaskLength() {
        return maskLength;
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

    public static PeerNetworkInterface deserialize(byte[] data, int length) throws IOException, ClassNotFoundException {
        if (length < 0 || length > data.length) {
            throw new IllegalArgumentException("Invalid length: " + length);
        }
        byte[] truncatedData = new byte[length];
        System.arraycopy(data, 0, truncatedData, 0, length);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(truncatedData);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (PeerNetworkInterface) ois.readObject();
        }
    }

    @Override
    public String toString() {
        return String.format("%s", localIPAddress);
    }
}
