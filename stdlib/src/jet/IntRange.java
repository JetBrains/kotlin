package jet;

public final class IntRange implements Range<Integer>, IntIterable, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(IntRange.class, false);

    private final int start;
    private final int count;

    public static final IntRange empty = new IntRange(0,0);

    public IntRange(int startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    @Override
    public boolean contains(Integer item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public IntIterator step(int step) {
        if(step < 0)
            return new MyIterator(getEnd(), -count, -step);
        else
            return new MyIterator(start, count, step);
    }

    public boolean getIsReversed() {
        return count < 0;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1;
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    public IntRange minus() {
        return new IntRange(getEnd(), -count);
    }

    @Override
    public IntIterator iterator() {
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

    public static IntRange count(int length) {
        return new IntRange(0, length);
    }

    private static class MyIterator extends IntIterator {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private int cur;
        private int step;
        private int count;

        private final boolean reversed;

        public MyIterator(int startValue, int count, int step) {
            cur = startValue;
            this.step = step;
            if(count < 0) {
                reversed = true;
                count = -count;
                startValue += count;
            }
            else {
                reversed = false;
            }
            this.count = count;
        }

        @Override
        public boolean getHasNext() {
            return count > 0;
        }

        @Override
        public int nextInt() {
            count -= step;
            if(reversed) {
                cur -= step;
                return cur + step;
            }
            else {
                cur += step;
                return cur - step;
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
