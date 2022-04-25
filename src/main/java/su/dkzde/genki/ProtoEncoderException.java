package su.dkzde.genki;

import java.io.IOException;

public class ProtoEncoderException extends RuntimeException {
    public ProtoEncoderException(IOException cause) {
        super(cause);
    }
}
