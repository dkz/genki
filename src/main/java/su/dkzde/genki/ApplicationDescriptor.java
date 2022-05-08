package su.dkzde.genki;

import java.io.IOException;

public record ApplicationDescriptor(
        byte[] identifier,
        byte[] authenticationCode) {

    static ApplicationDescriptor decode(ByteStream source) throws IOException {
        source.nextByte();
        byte[] identifier = source.nextByteSequence(new byte[8]);
        byte[] authenticationCode = source.nextByteSequence(new byte[3]);
        return new ApplicationDescriptor(identifier, authenticationCode);
    }

    static void encode(ApplicationDescriptor descriptor, ByteSink target) throws IOException {
        target.nextUnsignedByte(11);
        target.nextByteSequence(descriptor.identifier());
        target.nextByteSequence(descriptor.authenticationCode());
    }
}
