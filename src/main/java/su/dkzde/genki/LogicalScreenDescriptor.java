package su.dkzde.genki;

import java.io.IOException;

public record LogicalScreenDescriptor(
        int logicalScreenWidth,
        int logicalScreenHeight,
        boolean globalColorTableUsed,
        int colorTableSize,
        byte colorResolution,
        byte colorTableSizeBits,
        int backgroundColorIndex,
        byte pixelAspectRatio) {

    static LogicalScreenDescriptor decode(ByteStream source) throws IOException {
        int logicalScreenWidth = source.nextUnsignedShort();
        int logicalScreenHeight = source.nextUnsignedShort();
        boolean globalColorTableUsed;
        byte colorResolution;
        byte colorTableSizeBits;
        int colorTableSize;
        {
            int packed = source.nextUnsignedByte();
            globalColorTableUsed = (packed & (1 << 7)) > 0;
            colorResolution = (byte) ((packed & (0b111 << 4)) >> 4);
            colorTableSizeBits = (byte) (packed & 0b111);
            colorTableSize = (1 << (1 + colorTableSizeBits));
        }
        int backgroundColorIndex = Byte.toUnsignedInt(source.nextByte());
        byte pixelAspectRatio = source.nextByte();
        return new LogicalScreenDescriptor(
                logicalScreenWidth,
                logicalScreenHeight,
                globalColorTableUsed,
                colorTableSize,
                colorResolution,
                colorTableSizeBits,
                backgroundColorIndex,
                pixelAspectRatio);
    }

    static void encode(LogicalScreenDescriptor descriptor, ByteSink target) throws IOException {
        target.nextUnsignedShort(descriptor.logicalScreenWidth());
        target.nextUnsignedShort(descriptor.logicalScreenHeight());
        int packed = descriptor.colorTableSizeBits() | descriptor.colorResolution() << 4;
        if (descriptor.globalColorTableUsed()) {
            packed |= 0b1 << 7;
        }
        target.nextUnsignedByte(packed);
        target.nextUnsignedByte(descriptor.backgroundColorIndex());
        target.nextUnsignedByte(descriptor.pixelAspectRatio());
    }
}
