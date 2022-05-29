package su.dkzde.genki;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class OutputByteSink implements ByteSink {

    private final DataOutputStream backend;

    public OutputByteSink(OutputStream backend) {
        this.backend = new DataOutputStream(backend);
    }

    @Override
    public void init() {}

    @Override
    public void close() throws IOException {
        backend.close();
    }

    @Override
    public void nextByteSequence(byte[] sequence) throws IOException {
        backend.write(sequence);
    }

    @Override
    public void nextUnsignedShort(int field) throws IOException {
        backend.write(0xff & (field));
        backend.write(0xff & (field >> 8));
    }

    @Override
    public void nextUnsignedByte(int field) throws IOException {
        backend.writeByte(field);
    }
}
