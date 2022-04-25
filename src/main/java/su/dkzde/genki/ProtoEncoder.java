package su.dkzde.genki;

import java.io.IOException;

public class ProtoEncoder implements AutoCloseable, ProtoVisitor {

    private final ByteSink backend;

    public ProtoEncoder(ByteSink target) {
        this.backend = target;
    }

    @Override
    public void close() throws Exception {
        backend.close();
    }

    @Override
    public void visitHeader(Version version) {
        try {
            backend.init();
            switch (version) {
                case gif87a -> backend.nextByteSequence(new byte[] {'G', 'I', 'F', '8', '7', 'a'});
                case gif89a -> backend.nextByteSequence(new byte[] {'G', 'I', 'F', '8', '9', 'a'});
            }
        } catch (IOException exception) {
            throw new ProtoEncoderException(exception);
        }
    }

    @Override
    public void visitLogicalScreenDescriptor(LogicalScreenDescriptor descriptor) {
        try {
            LogicalScreenDescriptor.encode(descriptor, backend);
        } catch (IOException exception) {
            throw new ProtoEncoderException(exception);
        }
    }

    @Override
    public void visitGraphicsControlExtension(GraphicsControlExtension extension) {
        try {
            backend.nextUnsignedByte(0x21);
            backend.nextUnsignedByte(0xf9);
            GraphicsControlExtension.encode(extension, backend);
        } catch (IOException exception) {
            throw new ProtoEncoderException(exception);
        }
    }

    @Override
    public void visitGlobalColorTable(int index, byte r, byte g, byte b) {
        try {
            backend.nextUnsignedByte(r);
            backend.nextUnsignedByte(g);
            backend.nextUnsignedByte(b);
        } catch (IOException exception) {
            throw new ProtoEncoderException(exception);
        }
    }

    @Override
    public ProtoImageVisitor visitImage(ImageDescriptor descriptor) {
        try {
            backend.nextUnsignedByte(0x2c);
            ImageDescriptor.encode(descriptor, backend);
            return imageEncoder;
        } catch (IOException exception) {
            throw new ProtoEncoderException(exception);
        }
    }

    private final ProtoImageVisitor imageEncoder = new ProtoImageVisitor() {
        @Override public void visitColorTable(int index, byte r, byte g, byte b) {
            try {
                backend.nextUnsignedByte(r);
                backend.nextUnsignedByte(g);
                backend.nextUnsignedByte(b);
            } catch (IOException exception) {
                throw new ProtoEncoderException(exception);
            }
        }
        @Override public void visitDataStart(int lzwCodeSize) {
            try {
                backend.nextUnsignedByte(lzwCodeSize);
            } catch (IOException exception) {
                throw new ProtoEncoderException(exception);
            }

        }
        @Override public void visitDataBlock(byte[] block) {
            try {
                backend.nextUnsignedByte(block.length);
                backend.nextByteSequence(block);
            } catch (IOException exception) {
                throw new ProtoEncoderException(exception);
            }
        }
        @Override public void visitEnd() {
            try {
                backend.nextUnsignedByte(0x00);
            } catch (IOException exception) {
                throw new ProtoEncoderException(exception);
            }
        }
    };

    @Override
    public void visitEnd() {
        try {
            backend.nextUnsignedByte(0x3b);
        } catch (IOException exception) {
            throw new ProtoEncoderException(exception);
        }
    }
}
