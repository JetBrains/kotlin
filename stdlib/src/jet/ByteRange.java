package jet;

public final class ByteRange implements Range<Byte>, ByteIterable, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(ByteRange.class, false);

    private final byte start;
    private final int count;

    public ByteRange(byte startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    public ByteRange(byte startValue, int count, boolean reversed) {
        this(startValue, reversed ? -count : count);
    }

    public ByteRange(byte startValue, int count, boolean reversed, int defaultMask) {
        this(startValue, reversed ? -count : count, (defaultMask & 4) == 0);
    }

    @Override
    public boolean contains(Byte item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean getIsReversed() {
        return count < 0;
    }

    public byte getStart() {
        return start;
    }

    public byte getEnd() {
        return (byte) (count < 0 ? start + count + 1: start+count-1);
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    public ByteIterator step(int step) {
        return new MyIterator(start, count, step);
    }

    public ByteRange minus() {
        return new ByteRange(getEnd(), -count);
    }

    @Override
    public ByteIterator iterator() {
        return new MyIterator(start, count, 1);
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }

    @Override
    public JetObject getOuterObject() {
        return null;
    }

    public static ByteRange count(int length) {
        return new ByteRange((byte) 0, length);
    }

    public static ByteRange rangeTo(byte from, byte to) {
        if(from > to) {
            return new ByteRange(to, from-to+1, true);
        }
        else {
            return new ByteRange(from, to-from+1);
        }
    }

    private static class MyIterator extends ByteIterator {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private byte cur;
        private int step;
        private int count;

        private final boolean reversed;

        public MyIterator(byte startValue, int count, int step) {
            cur = startValue;
            this.step = step;
            reversed = count < 0;
            this.count = reversed ? -count : count;
        }

        @Override
        public boolean getHasNext() {
            return count > 0;
        }

        @Override
        public byte nextByte() {
            count -= step;
            if(reversed) {
                cur -= step;
                return (byte) (cur + step);
            }
            else {
                cur += step;
                return (byte) (cur - step);
            }
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }
}
