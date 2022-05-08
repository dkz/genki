package su.dkzde.genki;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * LZW encoder and decoder implementations.
 *
 * Concrete classes are too low-level to be accessed directly, hence it has package-level privacy set.
 * On top of that, the implementation of encoding and decoding algorithms are too fragile, since underlying
 * code is assuming it is used in a specified way, with a specific combination of calls.
 */
class LZW {
    private LZW() {}

    public static DataDecoder makeDecoder() {
        return makeDecoder(1 << 18, 1 << 10);
    }

    public static DataDecoder makeDecoder(int bufferCap, int stackCap) {
        return new Decoder(bufferCap, stackCap);
    }

    public static DataEncoder makeEncoder() {
        return new Encoder();
    }

    /**
     * Decodes a data block into an array of indices. Usage:
     * <pre>
     *     LZW.Decoder decoder = LZW.getDecoder();
     *     decoder.initialize(lzwMinimalCodeSize);
     *     while (moreInput) {
     *         int[] chunk = decoder.decode(block);
     *         process(chunk);
     *     }
     *     decoder.close();
     * </pre>
     */
    private static final class Decoder implements DataDecoder {

        private Decoder(int bufferCap, int stackCap) {
            o_buffer = new int[bufferCap];
            backrefStack = new int[stackCap];
        }

        @Override
        public void initialize(int mcs) {
            p = c_null;
            o_ptr = 0;
            i1_buffer = null;
            i1_ptr = 0;
            i_buffer = null;
            i_ptr = 0;
            initDictionary(mcs);
        }

        @Override
        public void dispose() {}

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
        private final int[] o_buffer;
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
                if (++t_ptr >= (1 << ccs)) {
                    if (ccs < 12) ccs++;
                }
            }
        }

        /**
         * Special stack for reading and writing code sequences.
         * As no memory allocations is a must, this stack stores the sequence of codes
         * to be inspected and written to output buffer.
         */
        private final int[] backrefStack;

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
        @Override
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

    /**
     * Encodes an index stream into LZW-compressed data sub-blocks. Usage:
     * <pre>
     *
     *     LZW.Encoder encoder = LZW.getEncoder();
     *     encoder.initialize(lzwMinimalCodeSize);
     *
     *     // While input is still available feed to encoder and call .encode until it eventually asks for more input
     *     // by returning a null:
     *     for (int[] block : blocks) {
     *         encoder.accept(block);
     *         for (byte[] db = encoder.encode(false); db != null; db = encoder.encode(false)) {
     *             processDataBlock(db);
     *         }
     *     }
     *
     *     // Signal to encoder that there is no more input by supplying true and consume the remaining data blocks:
     *     for (byte[] db = encoder.encode(true); db != null; db = encoder.encode(true)) {
     *         processDataBlock(db);
     *     }
     *     encoder.close();
     *
     * </pre>
     */
    private static final class Encoder implements DataEncoder {
        private Encoder() {}

        @Override
        public void initialize(int mcs) {
            eof_emitted = false;
            o_ptr = 0;
            ov_ptr = 0;
            i0_buffer = null;
            i0_ptr = 0;
            i_buffer = null;
            i_ptr = 0;
            initDictionary(mcs);
        }

        @Override
        public void dispose() {}

        private int o_ptr;
        private final byte[] o_buffer = new byte[255];
        private int ov_ptr = 0;
        private final byte[] ov_buffer = new byte[16];
        private boolean eof_emitted;
        /**
         * @return whether block is completed and should be emitted by a call to the encoder,
         * in case codes cannot be written to the output buffer, write them to the overflow
         * buffer to copy it back later.
         */
        private boolean write(int c) {

            int remain = 8 * o_buffer.length - o_ptr;
            if (remain <= 0) {
                insertCode(c, ccs, ov_ptr, ov_buffer);
                ov_ptr += ccs;
                return true;
            } else if (ov_ptr > 0) {
                System.arraycopy(ov_buffer, 0, o_buffer, 0, ov_buffer.length);
                insertCode(c, ccs, ov_ptr, o_buffer);
                o_ptr = ov_ptr + ccs;
                ov_ptr = 0;
                return false;
            }

            if (remain > ccs) {
                insertCode(c, ccs, o_ptr, o_buffer);
                o_ptr += ccs;
                return false;
            } else if (remain < ccs) {
                insertCode(c, remain, o_ptr, o_buffer);
                int rc = c >> remain;
                int rcs = ccs - remain;
                insertCode(rc, rcs, ov_ptr, ov_buffer);
                ov_ptr += rcs;
                o_ptr += remain;
                return true;
            } else {
                insertCode(c, ccs, o_ptr, o_buffer);
                o_ptr += ccs;
                return true;
            }
        }
        private byte[] output() {
            int cb = o_ptr/8;
            int rb = o_ptr%8;
            if (rb > 0) {
                rb = 8 - rb;
            }
            if (eof_emitted) {
                if (o_ptr > 0) {
                    if (cb < o_buffer.length) {
                        if (rb > 0) {
                            insertCode(0, rb, o_ptr, o_buffer);
                            o_ptr = 0;
                            return Arrays.copyOfRange(o_buffer, 0, 1 + cb);
                        } else {
                            o_ptr = 0;
                            return Arrays.copyOfRange(o_buffer, 0, cb);
                        }
                    } else {
                        o_ptr = 0;
                        return o_buffer;
                    }
                } else if (ov_ptr > 0) {
                    int ocb = ov_ptr/8;
                    int orb = ov_ptr%8;
                    if (orb > 0) {
                        orb = 8 - orb;
                    }
                    if (orb > 0) {
                        insertCode(0, ocb, ov_ptr, o_buffer);
                    }
                    ov_ptr = 0;
                    return Arrays.copyOfRange(ov_buffer, 0, 1 + ocb);
                } else {
                    return null;
                }
            } else {
                if (cb < o_buffer.length) {
                    if (rb > 0) {
                        insertCode(0, rb, o_ptr, o_buffer);
                        o_ptr = 0;
                        return Arrays.copyOfRange(o_buffer, 0, 1 + cb);
                    } else {
                        o_ptr = 0;
                        return Arrays.copyOfRange(o_buffer, 0, cb);
                    }
                } else {
                    o_ptr = 0;
                    return o_buffer;
                }
            }
        }

        private @Nullable int[] i0_buffer = null;
        private int i0_ptr;
        private int[] i_buffer;
        private int i_ptr;

        @Override
        public void accept(int[] chunk) {
            i0_buffer = i_buffer;
            i0_ptr = i_ptr;
            i_buffer = chunk;
            i_ptr = 0;
        }

        /** Advance input pointer by {@code s} index entries. */
        private void advance(int s) {
            if (i0_buffer != null) {
                int remain = i0_buffer.length - i0_ptr;
                if (s < remain) {
                    i0_ptr += s;
                    return;
                } else {
                    s -= remain;
                    i0_buffer = null;
                }
            }
            i_ptr += s;
        }
        /** Return index that is {@code i} indexes ahead of current pointer position, or {@code -1} if buffer is too short */
        private int lookup(int i) {
            if (i0_buffer != null) {
                if (i0_ptr + i < i0_buffer.length) {
                    return i0_buffer[i0_ptr + i];
                } else {
                    i -= i0_buffer.length - i0_ptr;
                }
            }
            if (i_ptr + i < i_buffer.length) {
                return i_buffer[i_ptr + i];
            } else {
                return -1;
            }
        }

        private static final int c_literal = -1;
        private static final int c_null = -2;

        private int its;
        private int mcs;
        private int ccs;
        private final int[] table = new int[3 * 4096];
        private int t_ptr = 0;
        private int c_clear;
        private int c_eof;
        private void initDictionary(int mcs) {
            this.mcs = mcs;
            this.ccs = 1 + mcs;
            this.its = 1 << mcs;
            for (int j = 0; j < its; j++) {
                table[3 * j] = c_literal;
                table[3 * j + 1] = j;
                table[3 * j + 2] = 0;
            }
            c_clear = its;
            c_eof = 1 + its;
            t_ptr = 2 + its;
            Arrays.fill(table, 3 * (its + 2), table.length, c_null);
            write(c_clear);
        }
        private boolean clearDictionary() {
            boolean yield = write(c_clear);
            ccs = 1 + mcs;
            t_ptr = 2 + its;
            Arrays.fill(table, 3 * (its + 2), table.length, c_null);
            return yield;
        }
        /** @return either previous element of the sequence or {@code c_literal} if entry is a part of initial table */
        private int lookupEntry(int e) {
            return table[3 * e];
        }
        /** @return output code of current entry element */
        private int lookupCode(int e) {
            return table[3 * e + 1];
        }
        /** @return lookup length of the current entry element */
        private int lookupLength(int e) {
            return table[3 * e + 2];
        }
        private int lookupSequenceLength(int e) {
            return 1 + table[3 * e + 2];
        }
        private boolean append(int c, int k) {
            if (t_ptr < 4096) {
                table[3 * t_ptr] = c;
                table[3 * t_ptr + 1] = k;
                table[3 * t_ptr + 2] = 1 + table[3 * c + 2];
                if (t_ptr++ >= (1 << ccs)) {
                    if (ccs < 12) ccs++;
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * While {@code eof} is false, it produces data blocks unless input buffer is exhausted,
         * after exhaustion returns {@code null}.
         * */
        @Override
        public byte[] encode(boolean eof) {

            while (true) {

                if (lookup(0) < 0) {
                    if (eof) {
                        if (!eof_emitted) {
                            write(c_eof);
                            eof_emitted = true;
                        }
                        return output();
                    } else {
                        return null;
                    }
                }

                // Lookup longest prefix:
                int lp = c_null;
                lookup:
                for (int j = t_ptr - 1; j > c_eof; j--) {
                    int e = j;
                    while (true) {
                        int el = lookupEntry(e);
                        int ec = lookupCode(e);
                        int en = lookupLength(e);
                        int ci = lookup(en);
                        if (ci < 0) {
                            if (eof) {
                                continue lookup;
                            } else {
                                return null;
                            }
                        }
                        if (lookup(en) == ec) {
                            if (el == c_literal) {
                                lp = j;
                                break lookup;
                            } else {
                                e = el;
                            }
                        } else {
                            continue lookup;
                        }
                    }
                }

                if (lp == c_null) {
                    lp = lookup(0);
                }

                int k = lookup(1 + lookupLength(lp));
                if (k < 0) {
                    if (!eof) {
                        return null;
                    }
                }
                advance(lookupSequenceLength(lp));

                boolean yieldBlock = write(lp);

                if (k >= 0) {
                    if (!append(lp, k)) {
                        yieldBlock |= clearDictionary();
                    }
                } else {
                    write(c_eof);
                    eof_emitted = true;
                    return output();
                }

                if (yieldBlock) {
                    return output();
                }
            }
        }
    }

    private static void insertCode(int c, int ccs, int ptr, byte[] block) {
        while (ccs > 0) {
            int bi = ptr/8;
            int cbp = ptr%8;
            int rb = Math.min(8 - cbp, ccs);
            int bs = c & ((1 << rb) - 1);
            block[bi] = (byte) ((block[bi] & ((1 << cbp) - 1)) | (bs << cbp));
            c = c >> rb;
            ccs -= rb;
            ptr += rb;
        }
    }
}
