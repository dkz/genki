package su.dkzde.genki;

import java.io.IOException;

public record GraphicsControlExtension(
        byte disposalMethod,
        boolean inputFlag,
        boolean transparencyFlag,
        int delayTime,
        int transparencyIndex) {

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
