package su.dkzde.genki;

import java.io.ByteArrayOutputStream;

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
}
