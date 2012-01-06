package jet;

public final class LongRange implements Range<Long>, LongIterable, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(IntRange.class, false);

    private final long start;
    private final long count;

    public LongRange(long startValue, long count) {
        this.start = startValue;
        this.count = count;
    }

    public LongRange(long startValue, long count, boolean reversed) {
        this(startValue, reversed ? -count : count);
    }

    public LongRange(int startValue, int count, boolean reversed, int defaultMask) {
        this(startValue, reversed ? -count : count, (defaultMask & 4) == 0);
    }

    public LongIterator step(long step) {
        return new MyIterator(start, count, step);
    }

    @Override
    public boolean contains(Long item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean getIsReversed() {
        return count < 0;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return start+count-1;
    }

    public long getSize() {
        return count < 0 ? -count : count;
    }

    public LongRange minus() {
        return new LongRange(getEnd(), -count);
    }

    @Override
    public LongIterator iterator() {
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

    public static LongRange count(int length) {
        return new LongRange(0, length);
    }

    private static class MyIterator extends LongIterator {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private long cur;
        private long step;
        private long count;

        private final boolean reversed;

        public MyIterator(long startValue, long count, long step) {
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
        public long nextLong() {
            count -= step;
            if(reversed) {
                cur -= step;
                return (cur + step);
            }
            else {
                cur += step;
                return (cur - step);
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
