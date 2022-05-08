package su.dkzde.genki;

public interface ProtoApplicationVisitor {
    void visitDataBlock(byte[] block);
    void visitEnd();
}
