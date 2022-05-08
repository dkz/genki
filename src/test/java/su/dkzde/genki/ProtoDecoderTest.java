package su.dkzde.genki;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ProtoDecoderTest {

    final byte[] input = ByteArrays.builder()
            // Header
            .block('G', 'I', 'F', '8', '9', 'a')
            // Logical screen descriptor
            .block(0x0a, 0x00, 0x0a, 0x00, 0x91, 0x00, 0x00)
            // Global color table
            .block(0xff, 0xff, 0xff)
            .block(0xff, 0x00, 0x00)
            .block(0x00, 0x00, 0xff)
            .block(0x00, 0x00, 0x00)
            // Graphic control extension
            .block(0x21, 0xf9, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00)
            // Image descriptor
            .block(0x2c, 0x00, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x0a, 0x00, 0x00)
            // Image data
            .block(0x02, 0x16)
            .block(0x8c, 0x2d, 0x99, 0x87)
            .block(0x2a, 0x1c, 0xdc, 0x33)
            .block(0xa0, 0x02, 0x75, 0xec)
            .block(0x95, 0xfa, 0xa8, 0xde)
            .block(0x60, 0x8c, 0x04, 0x91)
            .block(0x4c, 0x01, 0x00)
            .block(0x3b)
            .array();

    final int[] index_data = new int[] {
            1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
            1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
            1, 1, 1, 1, 1, 2, 2, 2, 2, 2,
            1, 1, 1, 0, 0, 0, 0, 2, 2, 2,
            1, 1, 1, 0, 0, 0, 0, 2, 2, 2,
            2, 2, 2, 0, 0, 0, 0, 1, 1, 1,
            2, 2, 2, 0, 0, 0, 0, 1, 1, 1,
            2, 2, 2, 2, 2, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 1, 1, 1, 1, 1
    };

    @Test void fullCycleEncoding() throws IOException {
        ByteArrays.ByteSinkImpl output = ByteArrays.channel();
        new ProtoDecoder(ByteArrays.asByteStream(input))
                .accept(new ProtoVisitorDecorator(new ProtoEncoder(output)) {

                    private LogicalScreenDescriptor lsd;

                    @Override public void visitLogicalScreenDescriptor(LogicalScreenDescriptor descriptor) {
                        this.lsd = descriptor;
                        super.visitLogicalScreenDescriptor(descriptor);
                    }

                    @Override public ProtoImageVisitor visitImage(ImageDescriptor descriptor) {
                        ImageVisitor encoder = new ImageEncoder(lsd, descriptor, super.visitImage(descriptor));
                        return new ImageDecoder(new ImageVisitorDecorator(encoder) {
                            @Override public void visitData(int[] block) {
                                Assertions.assertArrayEquals(block, index_data);
                                super.visitData(block);
                            }
                        });
                    }
                });
        Assertions.assertArrayEquals(input, output.array());
    }

    @Test void sampleDecoder() throws IOException {

        ByteArrays.ByteSinkImpl output = ByteArrays.channel();
        new ProtoDecoder(ByteArrays.asByteStream(input))
                .accept(new ProtoVisitorDecorator(new ProtoEncoder(output)) {
                    @Override public void visitHeader(Version version) {
                        super.visitHeader(version);
                        Assertions.assertEquals(Version.gif89a, version);
                    }
                    @Override public void visitLogicalScreenDescriptor(LogicalScreenDescriptor descriptor) {
                        super.visitLogicalScreenDescriptor(descriptor);
                        Assertions.assertEquals(10, descriptor.logicalScreenHeight());
                        Assertions.assertEquals(10, descriptor.logicalScreenWidth());
                        Assertions.assertEquals(4, descriptor.colorTableSize());
                        Assertions.assertEquals(0, descriptor.backgroundColorIndex());
                        Assertions.assertEquals(0, descriptor.pixelAspectRatio());
                        Assertions.assertTrue(descriptor.globalColorTableUsed());
                    }
                    @Override public void visitGraphicsControlExtension(GraphicsControlExtension extension) {
                        super.visitGraphicsControlExtension(extension);
                        Assertions.assertEquals(0, extension.disposalMethod());
                        Assertions.assertEquals(0, extension.transparencyIndex());
                        Assertions.assertEquals(0, extension.delayTime());
                        Assertions.assertFalse(extension.transparencyFlag());
                        Assertions.assertFalse(extension.inputFlag());
                    }
                    @Override public ProtoImageVisitor visitImage(ImageDescriptor descriptor) {
                        Assertions.assertEquals(10, descriptor.imageHeight());
                        Assertions.assertEquals(10, descriptor.imageWidth());
                        Assertions.assertEquals(0, descriptor.imageLeftPosition());
                        Assertions.assertEquals(0, descriptor.imageTopPosition());
                        Assertions.assertFalse(descriptor.localColorTableUsed());
                        return new ProtoImageVisitorDecorator(super.visitImage(descriptor)) {
                            LZW.Decoder decoder;
                            @Override public void visitDataStart(int lzwCodeSize) {
                                super.visitDataStart(lzwCodeSize);
                                decoder = LZW.getDecoder();
                                decoder.initialize(lzwCodeSize);
                            }
                            @Override public void visitDataBlock(byte[] block) {
                                super.visitDataBlock(block);
                                int[] indexes = decoder.decode(block);
                                Assertions.assertArrayEquals(indexes, index_data);
                            }
                            @Override public void visitEnd() {
                                super.visitEnd();
                                decoder.close();
                            }
                        };
                    }
                });

        Assertions.assertArrayEquals(input, output.array());
    }
}
