package su.dkzde.genki;

public abstract class ImageVisitorDecorator implements ImageVisitor {

    private final ImageVisitor downstream;

    public ImageVisitorDecorator(ImageVisitor downstream) {
        this.downstream = downstream;
    }

    @Override
    public void visitColorTable(int index, byte r, byte g, byte b) {
        downstream.visitColorTable(index, r, g, b);
    }

    @Override
    public void visitDataStart() {
        downstream.visitDataStart();
    }

    @Override
    public void visitData(int[] block) {
        downstream.visitData(block);
    }

    @Override
    public void visitDataEnd() {
        downstream.visitDataEnd();
    }
}
