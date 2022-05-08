package su.dkzde.genki;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;

public class ChannelByteSink implements ByteSink {

    private final ByteBuffer buffer;
    private final WritableByteChannel backend;

    public ChannelByteSink(WritableByteChannel backend, ByteBuffer buffer) {
        this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.backend = backend;
    }

    @Override
    public void init() {}

    @Override
    public void close() throws IOException {
        buffer.flip();
        while (buffer.hasRemaining()) {
            backend.write(buffer);
        }
        backend.close();
    }

    @Override
    public void nextByteSequence(byte[] sequence) throws IOException {
        if (buffer.remaining() >= sequence.length) {
            buffer.put(sequence);
        } else {
            buffer.flip();
            backend.write(buffer);
            buffer.compact();
            buffer.put(sequence);
        }
    }

    @Override
    public void nextUnsignedShort(int field) throws IOException {
        if (buffer.remaining() >= 2) {
            buffer.putShort((short) field);
        } else {
            buffer.flip();
            backend.write(buffer);
            buffer.compact();
            buffer.putShort((short) field);
        }
    }

    @Override
    public void nextUnsignedByte(int field) throws IOException {
        if (buffer.hasRemaining()) {
            buffer.put((byte) field);
        } else {
            buffer.flip();
            backend.write(buffer);
            buffer.compact();
            buffer.put((byte) field);
        }
    }
}
