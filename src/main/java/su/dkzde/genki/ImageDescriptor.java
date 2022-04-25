package su.dkzde.genki;

import java.io.IOException;

public record ImageDescriptor(
        int imageLeftPosition,
        int imageTopPosition,
        int imageWidth,
        int imageHeight,
        boolean localColorTableUsed,
        boolean interlacingUsed,
        byte colorTableSizeBits,
        int colorTableSize) {

    static ImageDescriptor decode(ByteStream source) throws IOException {
        int imageLeftPosition = source.nextUnsignedShort();
        int imageTopPosition = source.nextUnsignedShort();
        int imageWidth = source.nextUnsignedShort();
        int imageHeight = source.nextUnsignedShort();
        boolean localColorTableUsed;
        boolean interlacingUsed;
        byte colorTableSizeBits;
        int colorTableSize;
        {
            int packed = source.nextUnsignedByte();
            localColorTableUsed = (packed & (0b1 << 7)) > 0;
            interlacingUsed = (packed & (0b1 << 6)) > 0;
            colorTableSizeBits = (byte) (packed & 0b111);
            colorTableSize = (1 << (1 + colorTableSizeBits));
        }
        return new ImageDescriptor(
                imageLeftPosition,
                imageTopPosition,
                imageWidth,
                imageHeight,
                localColorTableUsed,
                interlacingUsed,
                colorTableSizeBits,
                colorTableSize);
    }

    static void encode(ImageDescriptor descriptor, ByteSink target) throws IOException {
        target.nextUnsignedShort(descriptor.imageLeftPosition());
        target.nextUnsignedShort(descriptor.imageTopPosition());
        target.nextUnsignedShort(descriptor.imageWidth());
        target.nextUnsignedShort(descriptor.imageHeight());
        int packed = descriptor.colorTableSizeBits();
        if (descriptor.localColorTableUsed()) {
            packed |= 0b1 << 7;
        }
        if (descriptor.interlacingUsed()) {
            packed |= 0b1 << 6;
        }
        target.nextUnsignedByte(packed);
    }
}
