package com.github.fevzibabaoglu.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class PeerFileMetadata implements Serializable, Cloneable {

    private static final int BUFFER_SIZE = 1024 * 1024;
    
    private final transient Path filePath;
    private final String filename;
    private final long fileSize;
    private final byte[] hash;

    public PeerFileMetadata(Path filePath) throws IOException, NoSuchAlgorithmException {
        this.filePath = filePath;
        this.filename = filePath.getFileName().toString();
        this.fileSize = filePath.toFile().length();
        this.hash = computeFileHash();
    }

    private PeerFileMetadata(String filename, long fileSize, byte[] hash) {
        this.filePath = null;
        this.filename = filename;
        this.fileSize = fileSize;
        this.hash = hash;
    }

    public String getFilename() {
        return filename;
    }

    public long getFileSize() throws IOException {
        return fileSize;
    }

    private byte[] computeFileHash() throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream fileInputStream = new FileInputStream(filePath.toString())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            // Read the file in chunks and update the digest
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        return digest.digest();
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
    public PeerFileMetadata clone() {
        try {
            return new PeerFileMetadata(
                this.filename,
                this.fileSize,
                this.hash
            );
        } catch (Exception e) {
            throw new AssertionError("Cloning PeerFileMetadata failed");
        }
    }

    @Override
    public String toString() {
        return filename;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PeerFileMetadata other = (PeerFileMetadata) obj;
        return Arrays.equals(hash, other.hash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(hash);
    }
}
