package com.github.fevzibabaoglu.file;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.fevzibabaoglu.network.file_transfer.FileChunkMessage;

public class FileManager {

    private final String inputPath;
    private final String outputPath;
    private final int chunkSize;

    public FileManager(String inputPath, String outputPath, int chunkSize) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.chunkSize = chunkSize;
    }

    // Reads a specific chunk from the file to send
    public byte[] getChunk(PeerFileMetadata fileMetadata, int chunkIndex) throws IOException {
        Path path = Paths.get(inputPath, fileMetadata.getFilename());

        // Calculate the start position of the requested chunk
        long startPosition = (long) chunkIndex * chunkSize;

        // Ensure the file exists and the chunk is within bounds
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + fileMetadata.getFilename());
        }

        long fileSize = Files.size(path);
        if (startPosition >= fileSize) {
            throw new IllegalArgumentException("Requested chunk is out of bounds.");
        }

        // Determine the size of the chunk to read (last chunk may be smaller)
        int bytesToRead = (int) Math.min(chunkSize, fileSize - startPosition);
        byte[] chunkData = new byte[bytesToRead];

        // Read the chunk dynamically
        try (RandomAccessFile raf = new RandomAccessFile(path.toString(), "r")) {
            raf.seek(startPosition);
            raf.readFully(chunkData);
            return chunkData;
        }
    }

    // Save an incoming chunk to disk
    public void saveChunk(FileChunkMessage fileChunkMessage) throws IOException {
        String chunkFilename = String.format("%s.%s", fileChunkMessage.getFileMetadata().getFilename(), fileChunkMessage.getChunkIndex());
        Path path = Paths.get(outputPath, chunkFilename);
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(path.toString()))) {
            out.write(fileChunkMessage.getChunkData());
        }
    }

    // Merge received chunks into a complete file
    public void mergeChunks(List<String> chunkFilenames, String outputFilename) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFilename))) {
            for (String chunkPath : chunkFilenames) {
                Path path = Paths.get(outputPath, chunkPath);
    
                // Ensure the chunk file exists
                if (!Files.exists(path)) {
                    throw new FileNotFoundException("Chunk not found: " + chunkPath);
                }
    
                // Read the entire chunk file and write it to the output file
                byte[] chunkData = Files.readAllBytes(path);
                out.write(chunkData);
            }
        }
    }

    // List files in a directory to be shared
    public Set<PeerFileMetadata> listSharedFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(inputPath))) {
            return paths.filter(Files::isRegularFile)
                        .map(path -> new PeerFileMetadata(path.toString()))
                        .collect(Collectors.toSet());
        }
    }

    public void createRandomFile(String filename, int size) throws IOException {
        Path filePath = Paths.get(inputPath, filename);
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {
            byte[] buffer = new byte[1024];
            Random random = new Random();

            for (int i = 0; i < size / buffer.length; i++) {
                random.nextBytes(buffer);
                fileOutputStream.write(buffer);
            }

            // Handle remaining bytes if size is not a multiple of buffer.length
            int remainingBytes = size % buffer.length;
            if (remainingBytes > 0) {
                buffer = new byte[remainingBytes];
                random.nextBytes(buffer);
                fileOutputStream.write(buffer);
            }

            System.out.println("Random file created: " + filePath);
        }
    }
}
