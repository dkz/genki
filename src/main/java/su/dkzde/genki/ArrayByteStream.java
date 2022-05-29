package su.dkzde.genki;

public class ArrayByteStream implements ByteStream {

    private final byte[] backend;
    private int cursor = 0;

    public ArrayByteStream(byte[] backend) {
        this.backend = backend;
    }

    @Override
    public void init() {}

    @Override
    public void close() {}

    @Override
    public byte nextByte() throws DataChannelException {
        if (cursor < backend.length) {
            return backend[cursor++];
        } else {
            throw new DataChannelException();
        }
    }

    @Override
    public byte[] nextByteSequence(byte[] sequence) throws DataChannelException {
        for (int j = 0; j < sequence.length; j++) {
            sequence[j] = nextByte();
        }
        return sequence;
    }

    @Override
    public int nextUnsignedByte() throws DataChannelException {
        return Byte.toUnsignedInt(nextByte());
    }

    @Override
    public int nextUnsignedShort() throws DataChannelException {
        return nextUnsignedByte() | nextUnsignedByte() << 2;
    }
}
