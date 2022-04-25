package su.dkzde.genki;

import java.util.*;

public class LZW {
    private LZW() {}

    private static final ThreadLocal<Decoder> decoder = ThreadLocal.withInitial(Decoder::new);

    public static Decoder getDecoder() {
        return decoder.get();
    }

    public static final class Decoder implements AutoCloseable {
        private Decoder() {}

        public void initialize(int mcs) {
            p = c_null;
            o_ptr = 0;
            initDictionary(mcs);
        }

        @Override
        public void close() {}

        /**
         * {@code i_buffer} stores the reference to current input sub-block,
         * {@link #set(byte[])} sets {@code i_ptr} back to zero
         * so that decoder can be shared instead of being re-allocated in memory.
         */
        private byte[] i_buffer;
        private int i_ptr = 0;
        /**
         * {@code i1_buffer} stores the previous block or data so decoder can join
         * codes that are being split by sub-block boundaries
         */
        private byte[] i1_buffer;
        private int i1_ptr;
        private void set(byte[] block) {
            i1_buffer = i_buffer;
            i1_ptr = i_ptr;
            i_buffer = block;
            i_ptr = 0;
        }
        private int read() {
            if (i1_buffer == null) {
                if (8 * i_buffer.length >= i_ptr + ccs) {
                    int code = LZW.nextCode(ccs, i_ptr, i_buffer);
                    i_ptr += ccs;
                    return code;
                } else {
                    return -1;
                }
            } else {
                int remain = 8 * i1_buffer.length - i1_ptr;
                if (remain > 0) {
                    int c1 = LZW.nextCode(remain, i1_ptr, i1_buffer);
                    int c2 = LZW.nextCode(ccs - remain, i_ptr, i_buffer);
                    i_ptr += ccs - remain;
                    i1_buffer = null;
                    i1_ptr = 0;
                    return c1 | (c2 << remain);
                } else {
                    i1_buffer = null;
                    i1_ptr = 0;
                    int code = LZW.nextCode(ccs, i_ptr, i_buffer);
                    i_ptr += ccs;
                    return code;
                }
            }
        }

        /**
         * {@code o_buffer} is preemptively allocated array used to record output stream,
         * so that each decode call produces max one allocation caused by copying {@code o_buffer}.
         */
        private final int[] o_buffer = new int[1 << 15];
        private int o_ptr = 0;
        private void write(int index) {
            o_buffer[o_ptr++] = index;
        }
        private int[] output() {
            int[] out = Arrays.copyOfRange(o_buffer, 0, o_ptr);
            o_ptr = 0;
            return out;
        }

        /**
         * {@code table} stores dictionary of LZW code sequences; sequences are stored by back-referencing
         * preceding code sequences or index literals; taking advantage of the byte-range of output indexes.
         * {@code table[2 * j]} is either a back-reference to previous table entry
         * i.e. {@code k} -- an entry index same as j, or one of the static constants listed below,
         * indicating termination of sequence or special codes.
         * {@code table[2 * j + 1]} is code as part of the sequence.
         */
        private final int[] table = new int[2 * 4096];

        /** Indicates that table entry is part of initial code table -- sequence terminator. */
        private static final int c_initial = -1;
        /** Clear code, indicates that call to {@link #clearDictionary()} is required. */
        private static final int c_clear = -2;
        /** Special end of input code, causes decoder to immediately return. */
        private static final int c_end = -3;
        /** Indicates that entry is still not set. */
        private static final int c_null = -4;

        /** Table pointer: points to an entry that is not yet occupied, hard capped at 4096. */
        private int t_ptr;
        /** Minimal code size. */
        private int mcs;
        /** Current code size. */
        private int ccs;
        /** Initial table size. */
        private int its;
        private void initDictionary(int mcs) {
            this.mcs = mcs;
            this.ccs = 1 + mcs;
            this.its = 1 << mcs;
            for (int j = 0; j < its; j++) {
                table[2 * j] = c_initial;
                table[2 * j + 1] = j;
            }
            table[2 * its] = c_clear;
            table[2 * (its + 1)] = c_end;
            t_ptr = 2 + its;
            Arrays.fill(table, 2 * (its + 2), table.length, c_null);
        }
        private void clearDictionary() {
            ccs = 1 + mcs;
            t_ptr = 2 + its;
            Arrays.fill(table, 2 * (its + 2), table.length, c_null);
        }
        private int lookupEntry(int c) {
            return table[2 * c];
        }
        private int lookupCode(int c) {
            return table[2 * c + 1];
        }
        private void append(int backref, int code) {
            if (t_ptr < 4096) {
                table[2 * t_ptr] = backref;
                table[2 * t_ptr + 1] = code;
                if (++t_ptr >= (1 << ccs)) ccs++;
            }
        }

        /**
         * Special stack for reading and writing code sequences.
         * As no memory allocations is a must, this stack stores the sequence of codes
         * to be inspected and written to output buffer.
         */
        private final int[] backrefStack = new int[1 << 8];

        /**
         * Build backref stack starting from the code {@code c}.
         * I.e. lookup entry at {@code c} and as long as entry is a back-reference to
         * another table's entry, write the code to stack and continue. Sequence is
         * terminated by code that is part of initial table.
         * @return recorded stack depth: the number of codes encountered
         */
        private int readBackrefStack(int c) {
            int backref = c;
            int backrefDepth = 0;
            while (true) {
                int backrefEntry = lookupEntry(backref);
                int backrefValue = lookupCode(backref);
                backrefStack[backrefDepth++] = backrefValue;
                if (backrefEntry == c_initial) {
                    return backrefDepth;
                } else {
                    backref = backrefEntry;
                }
            }
        }

        /** @return value from the top of the stack, given the depth is d */
        private int stackValue(int d) {
            return backrefStack[d - 1];
        }

        /** Copy backref stack to output buffer given the stack depth {@code d} */
        private void writeBackrefStack(int d) {
            for (int j = d; j > 0; j--) {
                write(stackValue(j));
            }
        }

        private int p = c_null;
        public int[] decode(byte[] block) {
            set(block);
            while (true) {
                int c = read();
                if (c < 0) {
                    return output();
                }
                int i = lookupEntry(c);
                switch (i) {
                    case c_end -> {
                        return output();
                    }
                    case c_clear -> {
                        p = c_null;
                        clearDictionary();
                    }
                    case c_null -> {
                        int d = readBackrefStack(p);
                        int k = stackValue(d);
                        writeBackrefStack(d);
                        write(k);
                        append(p, k);
                        p = c;
                    }
                    case c_initial -> {
                        write(c);
                        if (p != c_null) {
                            append(p, c);
                        }
                        p = c;
                    }
                    default -> {
                        int d = readBackrefStack(c);
                        int k = stackValue(d);
                        writeBackrefStack(d);
                        if (p != c_null) {
                            append(p, k);
                        }
                        p = c;
                    }
                }
            }
        }
    }

    /** @return the code of size {@code ccs} at {@code ptr} in specified byte block */
    private static int nextCode(int ccs, int ptr, byte[] block) {
        int c = 0;
        int clb = 0;
        int rcb = ccs;
        while (rcb > 0) {
            int cb = Byte.toUnsignedInt(block[ptr/8]);
            int cbp = ptr%8;
            int bs = Math.min(8 - cbp, rcb);
            int mask = ((1 << bs) - 1) << cbp;
            c |= ((cb & mask) >> cbp) << clb;
            clb += bs;
            ptr += bs;
            rcb -= bs;
        }
        return c;
    }
}
