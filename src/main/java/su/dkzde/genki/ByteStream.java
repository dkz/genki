package su.dkzde.genki;

import java.io.IOException;

public interface ByteStream extends AutoCloseable {

    void init() throws IOException;

    @Override void close() throws IOException;

    byte nextByte() throws IOException, DataChannelException;

    byte[] nextByteSequence(byte[] sequence) throws IOException, DataChannelException;

    int nextUnsignedByte() throws IOException, DataChannelException;

    int nextUnsignedShort() throws IOException, DataChannelException;
}
