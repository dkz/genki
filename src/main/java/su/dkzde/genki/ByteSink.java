package su.dkzde.genki;

import java.io.IOException;

public interface ByteSink extends AutoCloseable {

    void init() throws IOException;

    @Override void close() throws IOException;

    void nextByteSequence(byte[] sequence) throws IOException;

    void nextUnsignedShort(int field) throws IOException;

    void nextUnsignedByte(int field) throws IOException;
}
