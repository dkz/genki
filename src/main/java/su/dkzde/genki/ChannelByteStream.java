package su.dkzde.genki;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

public class ChannelByteStream implements ByteStream {

    private final ByteBuffer buffer;
    private final ReadableByteChannel backend;

    public ChannelByteStream(ReadableByteChannel source, ByteBuffer buffer) {
        this.buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.backend = source;
    }

    @Override
    public void close() throws IOException {
        backend.close();
    }

    @Override
    public void init() throws IOException {
        backend.read(buffer);
        buffer.flip();
    }

    @Override
    public byte nextByte() throws IOException {
        if (buffer.hasRemaining()) {
            return buffer.get();
        } else {
            buffer.clear();
            if (backend.read(buffer) > 0) {
                buffer.flip();
                return buffer.get();
            } else {
                throw new DataChannelException();
            }
        }
    }

    @Override
    public byte[] nextByteSequence(byte[] sequence) throws IOException {
        if (buffer.remaining() >= sequence.length) {
            buffer.get(sequence);
            return sequence;
        } else {
            buffer.compact();
            backend.read(buffer);
            buffer.flip();
            if (buffer.remaining() >= sequence.length) {
                buffer.get(sequence);
                return sequence;
            } else {
                throw new DataChannelException();
            }
        }
    }

    @Override
    public int nextUnsignedByte() throws IOException {
        return Byte.toUnsignedInt(nextByte());
    }

    @Override public int nextUnsignedShort() throws IOException {
        if (buffer.remaining() >= 2) {
            return Short.toUnsignedInt(buffer.getShort());
        } else {
            buffer.compact();
            backend.read(buffer);
            buffer.flip();
            if (buffer.remaining() >= 2) {
                return Short.toUnsignedInt(buffer.getShort());
            } else {
                throw new DataChannelException();
            }
        }
    }
}
