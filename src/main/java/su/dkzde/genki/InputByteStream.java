package su.dkzde.genki;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputByteStream implements ByteStream {

    private final DataInputStream backend;

    public InputByteStream(InputStream backend) {
        this.backend = new DataInputStream(backend);
    }

    @Override public void init() {}

    @Override
    public void close() throws IOException {
        backend.close();
    }

    @Override
    public byte nextByte() throws IOException {
        return backend.readByte();
    }

    @Override
    public byte[] nextByteSequence(byte[] sequence) throws IOException, DataChannelException {
        int consumed = backend.read(sequence);
        if (consumed < sequence.length) {
            throw new DataChannelException();
        } else {
            return sequence;
        }
    }

    @Override
    public int nextUnsignedByte() throws IOException, DataChannelException {
        return backend.readUnsignedByte();
    }

    @Override
    public int nextUnsignedShort() throws IOException, DataChannelException {
        return nextUnsignedByte() | nextUnsignedByte() << 2;
    }
}
