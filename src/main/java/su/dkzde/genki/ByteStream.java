package su.dkzde.genki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public interface ByteStream extends AutoCloseable {

    void init() throws IOException;

    @Override void close() throws IOException;

    byte nextByte() throws IOException, DataChannelException;

    byte[] nextByteSequence(byte[] sequence) throws IOException, DataChannelException;

    int nextUnsignedByte() throws IOException, DataChannelException;

    int nextUnsignedShort() throws IOException, DataChannelException;

    static ByteStream from(Path path) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
        return from(channel, ByteBuffer.allocate(1 << 12));
    }

    static ByteStream from(ReadableByteChannel channel, ByteBuffer buffer) {
        return new ChannelByteStream(channel, buffer);
    }

    static ByteStream from(InputStream stream) {
        return new InputByteStream(stream);
    }

    static ByteStream from(byte[] array) {
        return new ArrayByteStream(array);
    }
}
