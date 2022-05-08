package su.dkzde.genki;

import javax.annotation.Nullable;
import java.io.IOException;

public final class ProtoDecoder implements AutoCloseable {

    private final ByteStream source;

    public ProtoDecoder(ByteStream source) {
        this.source = source;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    public void accept(ProtoVisitor visitor) throws IOException {
        source.init();
        byte[] header = source.nextByteSequence(new byte[6]);
        switch (new String(header)) {
            case "GIF87a" -> visitor.visitHeader(Version.gif87a);
            case "GIF89a" -> visitor.visitHeader(Version.gif89a);
            default -> throw new DataChannelException();
        }
        LogicalScreenDescriptor lsd = LogicalScreenDescriptor.decode(source);
        visitor.visitLogicalScreenDescriptor(lsd);
        if (lsd.globalColorTableUsed()) {
            for (int index = 0; index < lsd.colorTableSize(); index++) {
                visitor.visitGlobalColorTable(index,
                        source.nextByte(),
                        source.nextByte(),
                        source.nextByte());
            }
        }
        while (true) {
            switch (source.nextUnsignedByte()) {
                // Extensions:
                case 0x21 -> {
                    switch (source.nextUnsignedByte()) {
                        case 0xf9 -> visitor.visitGraphicsControlExtension(GraphicsControlExtension.decode(source));
                        // Plain text extension and application extension, skip it:
                        case 0x01 -> skipExtensionBlock();
                        case 0xff -> readApplicationExtension(visitor.visitApplication(ApplicationDescriptor.decode(source)));
                        // Comment extension, skip it:
                        case 0xfe -> {
                            int bs = source.nextUnsignedByte();
                            while (bs > 0) {
                                source.nextByteSequence(new byte[bs]);
                                bs = source.nextUnsignedByte();
                            }
                        }
                    }
                }
                // Image:
                case 0x2c -> {
                    ImageDescriptor descriptor = ImageDescriptor.decode(source);
                    readImage(descriptor, visitor.visitImage(descriptor));
                }
                // Trailer byte:
                case 0x3b -> {
                    visitor.visitEnd();
                    return;
                }
            }
        }
    }

    private void skipExtensionBlock() throws IOException {
        int blockSize = source.nextUnsignedByte();
        while (blockSize-- > 0) {
            source.nextByte();
        }
        int bs = source.nextUnsignedByte();
        while (bs > 0) {
            source.nextByteSequence(new byte[bs]);
            bs = source.nextUnsignedByte();
        }
    }

    private void readApplicationExtension(@Nullable ProtoApplicationVisitor visitor) throws IOException {
        int bs = source.nextUnsignedByte();
        while (bs > 0) {
            byte[] block = source.nextByteSequence(new byte[bs]);
            if (visitor != null) {
                visitor.visitDataBlock(block);
            }
            bs = source.nextUnsignedByte();
        }
        if (visitor != null) {
            visitor.visitEnd();
        }
    }

    private void readImage(ImageDescriptor descriptor, @Nullable ProtoImageVisitor visitor) throws IOException {
        if (descriptor.localColorTableUsed()) {
            for (int index = 0; index < descriptor.colorTableSize(); index++) {
                byte r = source.nextByte();
                byte g = source.nextByte();
                byte b = source.nextByte();
                if (visitor != null) {
                    visitor.visitColorTable(index, r, g, b);
                }
            }
        }
        int lzwCodeSize = source.nextUnsignedByte();
        if (visitor != null) {
            visitor.visitDataStart(lzwCodeSize);
        }
        int bs = source.nextUnsignedByte();
        while (bs > 0) {
            byte[] block = source.nextByteSequence(new byte[bs]);
            if (visitor != null) {
                visitor.visitDataBlock(block);
            }
            bs = source.nextUnsignedByte();
        }
        if (visitor != null) {
            visitor.visitEnd();
        }
    }
}
