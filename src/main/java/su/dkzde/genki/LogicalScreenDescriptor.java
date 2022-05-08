package su.dkzde.genki;

import java.io.IOException;

public record LogicalScreenDescriptor(
        int logicalScreenWidth,
        int logicalScreenHeight,
        boolean globalColorTableUsed,
        byte colorResolution,
        byte colorTableSizeBits,
        int backgroundColorIndex,
        byte pixelAspectRatio) {

    public int colorTableSize() {
        return (1 << (1 + colorTableSizeBits));
    }

    public Builder copy() {
        return new Builder()
                .setLogicalScreenWidth(logicalScreenWidth)
                .setLogicalScreenHeight(logicalScreenHeight)
                .setGlobalColorTableUsed(globalColorTableUsed)
                .setColorResolution(colorResolution)
                .setColorTableSizeBits(colorTableSizeBits)
                .setBackgroundColorIndex(backgroundColorIndex)
                .setPixelAspectRatio(pixelAspectRatio);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int logicalScreenWidth;
        private int logicalScreenHeight;
        private boolean globalColorTableUsed;
        private byte colorResolution;
        private byte colorTableSizeBits;
        private int backgroundColorIndex;
        private byte pixelAspectRatio;
        private Builder() {}
        public Builder setLogicalScreenWidth(int width) {
            this.logicalScreenWidth = width;
            return this;
        }
        public Builder setLogicalScreenHeight(int height) {
            this.logicalScreenHeight = height;
            return this;
        }
        public Builder setGlobalColorTableUsed(boolean used) {
            this.globalColorTableUsed = used;
            return this;
        }
        public Builder setColorResolution(byte resolution) {
            this.colorResolution = resolution;
            return this;
        }
        public Builder setColorTableSizeBits(byte bits) {
            this.colorTableSizeBits = bits;
            return this;
        }
        public Builder setBackgroundColorIndex(int index) {
            this.backgroundColorIndex = index;
            return this;
        }
        public Builder setPixelAspectRatio(byte ratio) {
            this.pixelAspectRatio = ratio;
            return this;
        }
        public LogicalScreenDescriptor build() {
            return new LogicalScreenDescriptor(
                    logicalScreenWidth,
                    logicalScreenHeight,
                    globalColorTableUsed,
                    colorResolution,
                    colorTableSizeBits,
                    backgroundColorIndex,
                    pixelAspectRatio);
        }
    }

    static LogicalScreenDescriptor decode(ByteStream source) throws IOException {
        int logicalScreenWidth = source.nextUnsignedShort();
        int logicalScreenHeight = source.nextUnsignedShort();
        boolean globalColorTableUsed;
        byte colorResolution;
        byte colorTableSizeBits;
        {
            int packed = source.nextUnsignedByte();
            globalColorTableUsed = (packed & (1 << 7)) > 0;
            colorResolution = (byte) ((packed & (0b111 << 4)) >> 4);
            colorTableSizeBits = (byte) (packed & 0b111);
        }
        int backgroundColorIndex = Byte.toUnsignedInt(source.nextByte());
        byte pixelAspectRatio = source.nextByte();
        return new LogicalScreenDescriptor(
                logicalScreenWidth,
                logicalScreenHeight,
                globalColorTableUsed,
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
