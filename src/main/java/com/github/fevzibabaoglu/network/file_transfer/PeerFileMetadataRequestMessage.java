package com.github.fevzibabaoglu.network.file_transfer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;

import com.github.fevzibabaoglu.file.PeerFileMetadata;
import com.github.fevzibabaoglu.network.Peer;

public class PeerFileMetadataRequestMessage implements Message, Serializable {

    private final Peer sender;
    private final Peer receiver;
    private final PeerFileMetadata fileMetadata;
    private final Set<Integer> chunkIndices;

    public PeerFileMetadataRequestMessage(Peer sender, Peer receiver, PeerFileMetadata fileMetadata, Set<Integer> chunkIndices) {
        this.sender = sender;
        this.receiver = receiver;
        this.fileMetadata = fileMetadata;
        this.chunkIndices = chunkIndices;
    }

    public Peer getSender() {
        return sender;
    }

    public Peer getReceiver() {
        return receiver;
    }

    public PeerFileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public Set<Integer> getChunkIndices() {
        return chunkIndices;
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            return bos.toByteArray();
        }
    }

    public static PeerFileMetadataRequestMessage deserialize(byte[] data, int length) throws IOException, ClassNotFoundException {
        if (length < 0 || length > data.length) {
            throw new IllegalArgumentException("Invalid length: " + length);
        }
        byte[] truncatedData = new byte[length];
        System.arraycopy(data, 0, truncatedData, 0, length);
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(truncatedData);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (PeerFileMetadataRequestMessage) ois.readObject();
        }
    }
}
