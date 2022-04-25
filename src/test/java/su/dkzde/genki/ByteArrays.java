package su.dkzde.genki;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteArrays {
    private ByteArrays() {}

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final ByteArrayOutputStream backend = new ByteArrayOutputStream();
        private Builder() {}
        public Builder block(char... unsignedBytes) {
            for (char c : unsignedBytes) {
                backend.write((byte) c);
            }
            return this;
        }
        public Builder block(int... unsignedBytes) {
            for (int b : unsignedBytes) {
                backend.write((byte) b);
            }
            return this;
        }
        public byte[] array() {
            return backend.toByteArray();
        }
    }

    public static ByteSinkImpl channel() {
        return new ByteSinkImpl();
    }

    public static final class ByteSinkImpl implements ByteSink {
        private final ByteArrayOutputStream backend = new ByteArrayOutputStream();
        @Override public void init() {}
        @Override public void close() {}
        @Override public void nextByteSequence(byte[] sequence) throws IOException {
            backend.write(sequence);
        }
        @Override public void nextUnsignedByte(int field) throws IOException {
            backend.write((byte) field);
        }
        @Override public void nextUnsignedShort(int field) throws IOException {
            backend.write(0xff & (field));
            backend.write(0xff & (field >> 8));
        }
        public byte[] array() {
            return backend.toByteArray();
        }
    }

    public static ByteStream asByteStream(byte[] backend) {
        return new ByteStream() {
            int cursor = 0;
            @Override public void init() {}
            @Override public void close() {}
            @Override public byte nextByte() throws DataChannelException {
                return backend[cursor++];
            }
            @Override public int nextUnsignedByte() throws DataChannelException {
                return Byte.toUnsignedInt(nextByte());
            }
            @Override public int nextUnsignedShort() throws IOException, DataChannelException {
                return nextByte() | nextByte() << 2;
            }
            @Override public byte[] nextByteSequence(byte[] sequence) throws DataChannelException {
                for (int j = 0; j < sequence.length; j++) {
                    sequence[j] = nextByte();
                }
                return sequence;
            }
        };
    }
}
