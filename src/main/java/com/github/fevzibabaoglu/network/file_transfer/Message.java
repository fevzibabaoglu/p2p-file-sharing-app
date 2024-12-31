package com.github.fevzibabaoglu.network.file_transfer;

import java.io.IOException;

import com.github.fevzibabaoglu.file.PeerFileMetadata;
import com.github.fevzibabaoglu.network.Peer;

public interface Message {
    byte[] serialize() throws IOException;
    Peer getSender();
    Peer getReceiver();
    PeerFileMetadata getFileMetadata();
}
