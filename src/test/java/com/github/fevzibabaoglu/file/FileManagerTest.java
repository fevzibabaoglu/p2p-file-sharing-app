package com.github.fevzibabaoglu.file;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class FileManagerTest {

    private static final String ORIGINAL_FILE = "test-image.jpg"; // Path to an image file for testing
    private static final String CHUNK_PREFIX = "test-image.chunk"; // Prefix for chunk files
    private static final String MERGED_FILE = "merged-test-image.jpg"; // Path for the merged output file
    private FileManager fileManager;

    @BeforeEach
    public void setUp() throws IOException {
        fileManager = new FileManager();

        // Create a dummy file if ORIGINAL_FILE doesn't exist
        Path path = Paths.get(ORIGINAL_FILE);
        if (!Files.exists(path)) {
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(ORIGINAL_FILE))) {
                byte[] data = new byte[1024 * 1024]; // 1 MB dummy data
                new Random().nextBytes(data);
                out.write(data);
            }
        }
    }

    @Test
    public void testFileChunkingAndMerging() throws IOException {
        // Split the file into chunks dynamically
        int chunkSequence = 1;
        List<String> chunkPaths = new ArrayList<>();

        while (true) {
            try {
                // Get the next chunk
                byte[] chunkData = fileManager.getChunk(ORIGINAL_FILE, chunkSequence);

                // Save the chunk as if it were received
                String chunkFileName = CHUNK_PREFIX + chunkSequence;
                fileManager.saveChunk(chunkFileName, chunkData, chunkData.length);

                // Track the chunk path
                chunkPaths.add(chunkFileName);
                chunkSequence++;
            } catch (IllegalArgumentException e) {
                // End of file reached
                break;
            }
        }

        System.out.println("Chunks created: " + chunkPaths);

        // Merge the chunks back into a single file
        fileManager.mergeChunks(chunkPaths, MERGED_FILE);

        // Verify the merged file exists
        Path mergedFilePath = Paths.get(MERGED_FILE);
        assertTrue(Files.exists(mergedFilePath), "Merged file was not created.");

        // Verify the size of the merged file matches the original
        long originalSize = Files.size(Paths.get(ORIGINAL_FILE));
        long mergedSize = Files.size(mergedFilePath);
        assertEquals(originalSize, mergedSize, "Merged file size does not match the original.");

        System.out.println("Merged file created: " + mergedFilePath);
    }

    @AfterEach
    public void tearDown() {
        System.out.println("Test is done.");
    }
}
