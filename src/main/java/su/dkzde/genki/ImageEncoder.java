package su.dkzde.genki;

public final class ImageEncoder implements ImageVisitor {

    private final ProtoImageVisitor backend;
    private final ImageDescriptor id;
    private final LogicalScreenDescriptor lsd;
    private LZW.Encoder encoder;

    public ImageEncoder(
            LogicalScreenDescriptor lsd,
            ImageDescriptor id,
            ProtoImageVisitor backend) {

        this.backend = backend;
        this.id = id;
        this.lsd = lsd;
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
        encoder = LZW.getEncoder();
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
        encoder.close();
        backend.visitEnd();
    }
}
