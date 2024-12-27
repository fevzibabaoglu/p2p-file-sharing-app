package com.github.fevzibabaoglu.network.file_transfer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.github.fevzibabaoglu.file.PeerFileMetadata;
import com.github.fevzibabaoglu.network.Peer;

public class FileChunkMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private final Peer sender;
    private final Peer receiver;
    private final PeerFileMetadata fileMetadata;
    private final int chunkIndex;
    private final byte[] chunkData;

    public FileChunkMessage(Peer sender, Peer receiver, PeerFileMetadata fileMetadata, int chunkIndex, byte[] chunkData) {
        this.sender = sender;
        this.receiver = receiver;
        this.fileMetadata = fileMetadata;
        this.chunkIndex = chunkIndex;
        this.chunkData = chunkData;
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

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getFilename() {
        return String.format("%s.%s", fileMetadata.getFilename(), chunkIndex);
    }

    public byte[] getChunkData() {
        return chunkData;
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            return bos.toByteArray();
        }
    }

    public static FileChunkMessage deserialize(byte[] data, int length) throws IOException, ClassNotFoundException {
        if (length < 0 || length > data.length) {
            throw new IllegalArgumentException("Invalid length: " + length);
        }
        byte[] truncatedData = new byte[length];
        System.arraycopy(data, 0, truncatedData, 0, length);
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(truncatedData);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (FileChunkMessage) ois.readObject();
        }
    }
}
