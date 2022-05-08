package su.dkzde.genki;

import javax.annotation.Nullable;

public interface ProtoVisitor {
    void visitHeader(Version version);
    @Nullable ProtoApplicationVisitor visitApplication(ApplicationDescriptor descriptor);
    void visitLogicalScreenDescriptor(LogicalScreenDescriptor descriptor);
    void visitGraphicsControlExtension(GraphicsControlExtension extension);
    void visitGlobalColorTable(int index, byte r, byte g, byte b);
    @Nullable ProtoImageVisitor visitImage(ImageDescriptor descriptor);
    void visitEnd();
}
