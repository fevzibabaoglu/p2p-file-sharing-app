package com.github.fevzibabaoglu.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class PeerFileMetadata implements Serializable {
    
    private final String filename;
    private final byte[] hash;

    // TODO implement hashing
    public PeerFileMetadata(String filename) {
        this.filename = filename;
        this.hash = null;
    }

    public String getFilename() {
        return filename;
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] serialize() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            return bos.toByteArray();
        }
    }

    public static PeerFileMetadata deserialize(byte[] data, int length) throws IOException, ClassNotFoundException {
        if (length < 0 || length > data.length) {
            throw new IllegalArgumentException("Invalid length: " + length);
        }
        byte[] truncatedData = new byte[length];
        System.arraycopy(data, 0, truncatedData, 0, length);
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(truncatedData);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (PeerFileMetadata) ois.readObject();
        }
    }

    @Override
    public String toString() {
        return filename;
    }
}
