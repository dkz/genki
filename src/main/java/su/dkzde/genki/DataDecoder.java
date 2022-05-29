package su.dkzde.genki;

public interface DataDecoder {

    void initialize(int mcs);

    int[] decode(byte[] block);

    void dispose();

    static DataDecoder makeLZW() {
        return LZW.makeDecoder();
    }

    static DataDecoder makeLZW(int bufferCap, int stackCap) {
        return LZW.makeDecoder(bufferCap, stackCap);
    }

}
