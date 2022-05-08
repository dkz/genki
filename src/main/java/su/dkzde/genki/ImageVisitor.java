package su.dkzde.genki;

public interface ImageVisitor {
    void visitColorTable(int index, byte r, byte g, byte b);
    void visitDataStart();
    void visitData(int[] block);
    void visitDataEnd();
}
