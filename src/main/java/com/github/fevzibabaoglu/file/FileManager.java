package com.github.fevzibabaoglu.file;

import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.fevzibabaoglu.network.file_transfer.FileChunkMessage;

public class FileManager {

    private String sourcePath;
    private String destinationPath;
    private final int chunkSize;
    private final List<Path> excludedPaths;

    public FileManager(String sourcePath, String destinationPath, int chunkSize) {
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.chunkSize = chunkSize;
        this.excludedPaths = new CopyOnWriteArrayList<>();
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setPaths(String inputPath, String outputPath) {
        this.sourcePath = inputPath;
        this.destinationPath = outputPath;
    }

    public void addExcludedPath(Path path) {
        excludedPaths.add(path);
    }

    public void removeExcludedPath(Path path) {
        excludedPaths.remove(path);
    }

    // Reads a specific chunk from the file to send
    public byte[] loadChunk(PeerFileMetadata fileMetadata, int chunkIndex) throws IOException {
        Path path = Paths.get(sourcePath, fileMetadata.getFilename());

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
    public PeerFileMetadata saveChunk(FileChunkMessage fileChunkMessage) throws IOException, NoSuchAlgorithmException {
        String chunkFilename = String.format("%s.%s", fileChunkMessage.getFileMetadata().getFilename(), fileChunkMessage.getChunkIndex());
        Path path = Paths.get(destinationPath, chunkFilename);
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(path.toString()))) {
            out.write(fileChunkMessage.getChunkData());
        }
        return new PeerFileMetadata(path);
    }

    // Merge received chunks into a complete file
    public void mergeChunks(List<String> chunkFilenames, String outputFilename) throws IOException, InterruptedException {
        Path outputPath = Paths.get(destinationPath, outputFilename);
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputPath.toString()))) {
            for (String chunkFilename : chunkFilenames) {
                Path path = Paths.get(destinationPath, chunkFilename);
    
                // Read the entire chunk file and write it to the output file
                byte[] chunkData = Files.readAllBytes(path);
                out.write(chunkData);

                // Delete the chunk file after writing
                Files.delete(path);
            }
        }
    }

    // List files in a directory to be shared
    public Set<PeerFileMetadata> listSharedFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(sourcePath))) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> excludedPaths.stream()
                    .noneMatch(excludedPath -> path.startsWith(excludedPath)))
                .map(path -> {
                    try {
                        return new PeerFileMetadata(path);
                    } catch (IOException | NoSuchAlgorithmException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }
    }

    public void createRandomFile(String filename, int size, long seed) throws IOException {
        Path filePath = Paths.get(sourcePath, filename);
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {
            byte[] buffer = new byte[1024];
            Random random = (seed == -1) ? new Random() : new Random(seed);

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
