package jet;

import jet.typeinfo.TypeInfo;

public final class LongRange implements Range<Long>, Iterable<Long>, JetObject {
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

    @Override
    public boolean contains(Long item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
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

    @Override
    public Iterator<Long> iterator() {
        return new MyIterator(start, count);
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }

    public static IntRange count(int length) {
        return new IntRange(0, length);
    }

    public static IntRange rangeTo(int from, int to) {
        if(from > to) {
            return new IntRange(to, from-to+1, true);
        }
        else {
            return new IntRange(from, to-from+1);
        }
    }

    private static class MyIterator implements Iterator<Long> {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private long cur;
        private long count;

        private final boolean reversed;

        public MyIterator(long startValue, long count) {
            cur = startValue;
            reversed = count < 0;
            this.count = reversed ? -count : count;
        }

        @Override
        public boolean getHasNext() {
            return count > 0;
        }

        @Override
        public Long next() {
            count--;
            if(reversed) {
                return cur--;
            }
            else {
                return cur++;
            }
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }
}
