package jet;

import jet.typeinfo.TypeInfo;

public final class LongRange implements Range<Long>, Iterable<Long>, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(IntRange.class, false);

    private final long startValue;
    private final long excludedEndValue;

    public LongRange(long startValue, long endValue) {
        this.startValue = startValue;
        this.excludedEndValue = endValue;
    }

    @Override
    public boolean contains(Long item) {
        if (item == null) return false;
        if (startValue < excludedEndValue) {
            return item >= startValue && item < excludedEndValue;
        }
        return item <= startValue && item > excludedEndValue;
    }

    public long getStart() {
        return startValue;
    }

    public long getEnd() {
        return excludedEndValue;
    }

    @Override
    public Iterator<Long> iterator() {
        return new MyIterator(startValue, excludedEndValue);
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }

    public static IntRange count(int length) {
        return new IntRange(0, length);
    }

    private static class MyIterator implements Iterator<Long> {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);
        private final long lastValue;

        private long cur;
        private boolean reversed;

        public MyIterator(long startValue, long endValue) {
            reversed = endValue <= startValue;
            this.lastValue = reversed ? startValue : endValue-1;
            cur = reversed ? endValue-1 : startValue;
        }

        @Override
        public boolean hasNext() {
            return reversed ? cur >= lastValue : cur <= lastValue;
        }

        @Override
        public Long next() {
            return reversed ? cur-- : cur++;
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            return typeInfo;
        }
    }
}
