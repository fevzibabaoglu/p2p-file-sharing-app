package com.github.fevzibabaoglu;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManager {

    private static final int CHUNK_SIZE = 256 * 1024; // 256 KB

    // Reads a specific chunk from the file to send
    public byte[] getChunk(String filePath, int chunkSequence) throws IOException {
        Path path = Paths.get(filePath);

        // Calculate the start position of the requested chunk
        long startPosition = (long) (chunkSequence - 1) * CHUNK_SIZE;

        // Ensure the file exists and the chunk is within bounds
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        long fileSize = Files.size(path);
        if (startPosition >= fileSize) {
            throw new IllegalArgumentException("Requested chunk is out of bounds.");
        }

        // Determine the size of the chunk to read (last chunk may be smaller)
        int bytesToRead = (int) Math.min(CHUNK_SIZE, fileSize - startPosition);
        byte[] chunkData = new byte[bytesToRead];

        // Read the chunk dynamically
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(startPosition); // Move the file pointer to the start of the chunk
            raf.readFully(chunkData);
            return chunkData;
        }
    }

    // Save an incoming chunk to disk
    public void saveChunk(String chunkFileName, byte[] buffer, int bytesRead) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(chunkFileName))) {
            out.write(buffer, 0, bytesRead);
        }
    }

    // Merge received chunks into a complete file
    public void mergeChunks(List<String> chunkPaths, String outputFile) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            for (String chunkPath : chunkPaths) {
                Path path = Paths.get(chunkPath);
    
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
    public List<String> listSharedFiles(String directoryPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            return paths.filter(Files::isRegularFile)
                        .map(path -> path.toString())
                        .collect(Collectors.toList());
        }
    }
}
