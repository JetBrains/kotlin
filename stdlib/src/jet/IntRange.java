package jet;

import jet.typeinfo.TypeInfo;

public final class IntRange implements Range<Integer>, Iterable<Integer>, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(IntRange.class, false);

    private final int start;
    private final int count;

    public IntRange(int startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    public IntRange(int startValue, int count, boolean reversed) {
        this(startValue, reversed ? -count : count);
    }

    @Override
    public boolean contains(Integer item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return start+count-1;
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    @Override
    public Iterator<Integer> iterator() {
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

    private static class MyIterator implements Iterator<Integer> {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private int cur;
        private int count;

        private final boolean reversed;

        public MyIterator(int startValue, int count) {
            cur = startValue;
            reversed = count < 0;
            this.count = reversed ? -count : count;
        }

        @Override
        public boolean getHasNext() {
            return count > 0;
        }

        @Override
        public Integer next() {
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
