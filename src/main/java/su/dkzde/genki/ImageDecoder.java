package su.dkzde.genki;

import java.util.function.Supplier;

public final class ImageDecoder implements ProtoImageVisitor {

    private final ImageVisitor backend;
    private final Supplier<DataDecoder> supplier;
    private DataDecoder decoder;

    public ImageDecoder(Supplier<DataDecoder> supplier, ImageVisitor visitor) {
        this.backend = visitor;
        this.supplier = supplier;
    }

    @Override
    public void visitColorTable(int index, byte r, byte g, byte b) {
        backend.visitColorTable(index, r, g, b);
    }

    @Override
    public void visitDataStart(int lzwCodeSize) {
        decoder = supplier.get();
        decoder.initialize(lzwCodeSize);
        backend.visitDataStart();
    }

    @Override
    public void visitDataBlock(byte[] block) {
        backend.visitData(decoder.decode(block));
    }

    @Override
    public void visitEnd() {
        decoder.dispose();
        backend.visitDataEnd();
    }
}
