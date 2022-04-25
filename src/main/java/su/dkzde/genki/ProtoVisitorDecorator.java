package su.dkzde.genki;

import javax.annotation.Nullable;

public abstract class ProtoVisitorDecorator implements ProtoVisitor {

    protected final ProtoVisitor downstream;

    public ProtoVisitorDecorator(ProtoVisitor downstream) {
        this.downstream = downstream;
    }

    @Override
    public void visitHeader(Version version) {
        downstream.visitHeader(version);
    }

    @Override
    public void visitLogicalScreenDescriptor(LogicalScreenDescriptor descriptor) {
        downstream.visitLogicalScreenDescriptor(descriptor);
    }

    @Override
    public void visitGraphicsControlExtension(GraphicsControlExtension extension) {
        downstream.visitGraphicsControlExtension(extension);
    }

    @Override
    public void visitGlobalColorTable(int index, byte r, byte g, byte b) {
        downstream.visitGlobalColorTable(index, r, g, b);
    }

    @Override
    public @Nullable ProtoImageVisitor visitImage(ImageDescriptor descriptor) {
        return downstream.visitImage(descriptor);
    }

    @Override
    public void visitEnd() {
        downstream.visitEnd();
    }
}
