package su.dkzde.genki;

public interface ProtoImageVisitor {
    void visitColorTable(int index, byte r, byte g, byte b);
    void visitDataStart(int lzwCodeSize);
    void visitDataBlock(byte[] block);
    void visitEnd();
}
