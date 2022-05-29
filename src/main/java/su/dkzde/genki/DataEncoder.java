package su.dkzde.genki;

public interface DataEncoder {

    void initialize(int mcs);

    void accept(int[] chunk);

    byte[] encode(boolean eof);

    void dispose();

    static DataEncoder makeEncoder() {
        return LZW.makeEncoder();
    }
}
