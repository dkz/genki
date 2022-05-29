package su.dkzde.genki;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public interface ByteSink extends AutoCloseable {

    void init() throws IOException;

    @Override void close() throws IOException;

    void nextByteSequence(byte[] sequence) throws IOException;

    void nextUnsignedShort(int field) throws IOException;

    void nextUnsignedByte(int field) throws IOException;

    static ByteSink from(Path path) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        return from(channel, ByteBuffer.allocate(1 << 12));
    }

    static ByteSink from(WritableByteChannel channel, ByteBuffer buffer) {
        return new ChannelByteSink(channel, buffer);
    }

    static ByteSink from(OutputStream stream) {
        return new OutputByteSink(stream);
    }
}
