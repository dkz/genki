package su.dkzde.genki;

import java.io.IOException;

public record ImageDescriptor(
        int imageLeftPosition,
        int imageTopPosition,
        int imageWidth,
        int imageHeight,
        boolean localColorTableUsed,
        boolean interlacingUsed,
        byte colorTableSizeBits) {

    public int colorTableSize() {
        return (1 << (1 + colorTableSizeBits));
    }

    public Builder copy() {
        return new Builder()
                .setImageLeftPosition(imageLeftPosition)
                .setImageTopPosition(imageTopPosition)
                .setImageWidth(imageWidth)
                .setImageHeight(imageHeight)
                .setLocalColorTableUsed(localColorTableUsed)
                .setInterlacingUsed(interlacingUsed)
                .setColorTableSizeBits(colorTableSizeBits);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int imageLeftPosition;
        private int imageTopPosition;
        private int imageWidth;
        private int imageHeight;
        private boolean localColorTableUsed;
        private boolean interlacingUsed;
        private byte colorTableSizeBits;
        private Builder() {}
        public Builder setImageLeftPosition(int position) {
            this.imageLeftPosition = position;
            return this;
        }
        public Builder setImageTopPosition(int position) {
            this.imageTopPosition = position;
            return this;
        }
        public Builder setImageWidth(int width) {
            this.imageWidth = width;
            return this;
        }
        public Builder setImageHeight(int height) {
            this.imageHeight = height;
            return this;
        }
        public Builder setLocalColorTableUsed(boolean used) {
            this.localColorTableUsed = used;
            return this;
        }
        public Builder setInterlacingUsed(boolean used) {
            this.interlacingUsed = used;
            return this;
        }
        public Builder setColorTableSizeBits(byte bits) {
            this.colorTableSizeBits = bits;
            return this;
        }
        public ImageDescriptor build() {
            return new ImageDescriptor(
                    imageLeftPosition,
                    imageTopPosition,
                    imageWidth,
                    imageHeight,
                    localColorTableUsed,
                    interlacingUsed,
                    colorTableSizeBits);
        }
    }

    static ImageDescriptor decode(ByteStream source) throws IOException {
        int imageLeftPosition = source.nextUnsignedShort();
        int imageTopPosition = source.nextUnsignedShort();
        int imageWidth = source.nextUnsignedShort();
        int imageHeight = source.nextUnsignedShort();
        boolean localColorTableUsed;
        boolean interlacingUsed;
        byte colorTableSizeBits;
        {
            int packed = source.nextUnsignedByte();
            localColorTableUsed = (packed & (0b1 << 7)) > 0;
            interlacingUsed = (packed & (0b1 << 6)) > 0;
            colorTableSizeBits = (byte) (packed & 0b111);
        }
        return new ImageDescriptor(
                imageLeftPosition,
                imageTopPosition,
                imageWidth,
                imageHeight,
                localColorTableUsed,
                interlacingUsed,
                colorTableSizeBits);
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
