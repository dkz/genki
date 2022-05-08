package su.dkzde.genki;

public interface DataDecoder {

    void initialize(int mcs);

    int[] decode(byte[] block);

    void dispose();

}
