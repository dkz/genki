package su.dkzde.genki;

public abstract class ProtoImageVisitorDecorator implements ProtoImageVisitor {

    private final ProtoImageVisitor downstream;

    public ProtoImageVisitorDecorator(ProtoImageVisitor downstream) {
        this.downstream = downstream;
    }

    @Override
    public void visitColorTable(int index, byte r, byte g, byte b) {
        downstream.visitColorTable(index, r, g, b);
    }

    @Override
    public void visitDataStart(int lzwCodeSize) {
        downstream.visitDataStart(lzwCodeSize);
    }

    @Override
    public void visitDataBlock(byte[] block) {
        downstream.visitDataBlock(block);
    }

    @Override
    public void visitEnd() {
        downstream.visitEnd();
    }
}
