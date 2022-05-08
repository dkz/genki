package su.dkzde.genki;

public final class ImageDecoder implements ProtoImageVisitor {

    private final ImageVisitor backend;
    private LZW.Decoder decoder;

    public ImageDecoder(ImageVisitor visitor) {
        this.backend = visitor;
    }

    @Override
    public void visitColorTable(int index, byte r, byte g, byte b) {
        backend.visitColorTable(index, r, g, b);
    }

    @Override
    public void visitDataStart(int lzwCodeSize) {
        decoder = LZW.getDecoder();
        decoder.initialize(lzwCodeSize);
        backend.visitDataStart();
    }

    @Override
    public void visitDataBlock(byte[] block) {
        backend.visitData(decoder.decode(block));
    }

    @Override
    public void visitEnd() {
        decoder.close();
        backend.visitDataEnd();
    }
}
