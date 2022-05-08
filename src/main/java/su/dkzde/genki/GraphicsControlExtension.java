package su.dkzde.genki;

import java.io.IOException;

public record GraphicsControlExtension(
        byte disposalMethod,
        boolean inputFlag,
        boolean transparencyFlag,
        int delayTime,
        int transparencyIndex) {

    public Builder copy() {
        return new Builder()
                .setDisposalMethod(disposalMethod)
                .setInputFlag(inputFlag)
                .setTransparencyFlag(transparencyFlag)
                .setDelayTime(delayTime)
                .setTransparencyIndex(transparencyIndex);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private byte disposalMethod;
        private boolean inputFlag;
        private boolean transparencyFlag;
        private int delayTime;
        private int transparencyIndex;
        private Builder() {}
        public Builder setDisposalMethod(byte method) {
            this.disposalMethod = method;
            return this;
        }
        public Builder setInputFlag(boolean flag) {
            this.inputFlag = flag;
            return this;
        }
        public Builder setTransparencyFlag(boolean flag) {
            this.transparencyFlag = flag;
            return this;
        }
        public Builder setDelayTime(int delay) {
            this.delayTime = delay;
            return this;
        }
        public Builder setTransparencyIndex(int index) {
            this.transparencyIndex = index;
            return this;
        }
        public GraphicsControlExtension build() {
            return new GraphicsControlExtension(
                    disposalMethod,
                    inputFlag,
                    transparencyFlag,
                    delayTime,
                    transparencyIndex);
        }
    }

    static GraphicsControlExtension decode(ByteStream source) throws IOException {
        if (source.nextByte() != 0x04) {
            throw new DataChannelException();
        }
        byte disposalMethod;
        boolean inputFlag;
        boolean transparencyFlag;
        {
            int packed = source.nextUnsignedByte();
            disposalMethod = (byte) ((packed & (0b111 << 2)) >> 2);
            inputFlag = (packed & 0b10) > 0;
            transparencyFlag = (packed & 0b1) > 0;
        }
        int delayTime = source.nextUnsignedShort();
        int transparencyIndex = source.nextUnsignedByte();
        if (source.nextByte() != 0x00) {
            throw new DataChannelException();
        }
        return new GraphicsControlExtension(
                disposalMethod,
                inputFlag,
                transparencyFlag,
                delayTime,
                transparencyIndex);
    }

    static void encode(GraphicsControlExtension extension, ByteSink target) throws IOException {
        target.nextUnsignedByte(0x04);
        int packed = extension.disposalMethod() << 2;
        if (extension.inputFlag()) {
            packed |= 0b10;
        }
        if (extension.transparencyFlag()) {
            packed |= 1;
        }
        target.nextUnsignedByte(packed);
        target.nextUnsignedShort(extension.delayTime());
        target.nextUnsignedByte(extension.transparencyIndex());
        target.nextUnsignedByte(0x00);
    }
}
