package com.github.fevzibabaoglu.file;

import org.junit.jupiter.api.*;

import com.github.fevzibabaoglu.network.file_transfer.FileChunkMessage;

import java.io.*;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class FileManagerTest {

    private static final String INPUT_PATH = "";
    private static final String OUTPUT_PATH = "";
    private static final int CHUNK_SIZE = 256 * 1024;
    private static final String ORIGINAL_FILE = "test-file"; // Path to a file for testing
    private static final String MERGED_FILE = "merged-test-file"; // Path for the merged output file

    private FileManager fileManager;
    private PeerFileMetadata fileMetadata;

    @BeforeEach
    public void setUp() throws IOException, NoSuchAlgorithmException {
        fileManager = new FileManager(INPUT_PATH, OUTPUT_PATH, CHUNK_SIZE);

        // Create a dummy file if ORIGINAL_FILE doesn't exist
        Path path = Paths.get(ORIGINAL_FILE);
        if (!Files.exists(path)) {
            fileManager.createRandomFile(ORIGINAL_FILE, 1024 * 1024 + 15);
        }
        fileMetadata = new PeerFileMetadata(path);
    }

    @Test
    public void testFileChunkingAndMerging() throws IOException, NoSuchAlgorithmException, InterruptedException {
        int chunkIndex = 0;
        List<Path> chunkPaths = new ArrayList<>();

        while (true) {
            try {
                // Get the next chunk
                byte[] chunkData = fileManager.loadChunk(fileMetadata, chunkIndex);
                FileChunkMessage fileChunkMessage = new FileChunkMessage(null, null, fileMetadata, chunkIndex, chunkData);
                String chunkFileName = fileChunkMessage.getFilename();
                
                // Save the chunk as if it were received
                fileManager.saveChunk(fileChunkMessage);

                // Track the chunk path
                Path path = Paths.get(OUTPUT_PATH, chunkFileName);
                chunkPaths.add(path);
                chunkIndex++;
            } catch (IllegalArgumentException e) {
                // End of file reached
                break;
            }
        }

        System.out.println("Chunks created: " + chunkPaths);

        // Merge the chunks back into a single file
        fileManager.mergeChunks(chunkPaths.stream().map(chunkPath -> {
            try {
                return new PeerFileMetadata(chunkPath);
            } catch (NoSuchAlgorithmException | IOException e) {
                return null;
            }
        }).filter(Objects::nonNull).toList(), MERGED_FILE);

        // Verify the merged file exists
        Path mergedFilePath = Paths.get(MERGED_FILE);
        assertTrue(Files.exists(mergedFilePath), "Merged file was not created.");

        // Verify the size of the merged file matches the original
        long originalSize = Files.size(Paths.get(fileMetadata.getFilename()));
        long mergedSize = Files.size(mergedFilePath);
        assertEquals(originalSize, mergedSize, "Merged file size does not match the original.");

        System.out.println("Merged file created: " + mergedFilePath);
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Test is done.");
    }
}
