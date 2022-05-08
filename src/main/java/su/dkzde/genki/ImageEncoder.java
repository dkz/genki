package su.dkzde.genki;

import java.util.function.Supplier;

public final class ImageEncoder implements ImageVisitor {

    private final ProtoImageVisitor backend;
    private final ImageDescriptor id;
    private final LogicalScreenDescriptor lsd;
    private final Supplier<DataEncoder> supplier;
    private DataEncoder encoder;

    public ImageEncoder(
            Supplier<DataEncoder> supplier,
            LogicalScreenDescriptor lsd,
            ImageDescriptor id,
            ProtoImageVisitor backend) {

        this.backend = backend;
        this.id = id;
        this.lsd = lsd;
        this.supplier = supplier;
    }

    @Override
    public void visitColorTable(int index, byte r, byte g, byte b) {
        backend.visitColorTable(index, r, g, b);
    }

    @Override
    public void visitDataStart() {
        final int mcs;
        if (id.localColorTableUsed()) {
            mcs = Math.max(2, 1 + id.colorTableSizeBits());
        } else {
            mcs = Math.max(2, 1 + lsd.colorTableSizeBits());
        }
        encoder = supplier.get();
        encoder.initialize(mcs);
        backend.visitDataStart(mcs);
    }

    @Override
    public void visitData(int[] block) {
        encoder.accept(block);
        for (byte[] db = encoder.encode(false); db != null; db = encoder.encode(false)) {
            backend.visitDataBlock(db);
        }
    }

    @Override
    public void visitDataEnd() {
        for (byte[] db = encoder.encode(true); db != null; db = encoder.encode(true)) {
            backend.visitDataBlock(db);
        }
        encoder.dispose();
        backend.visitEnd();
    }
}
