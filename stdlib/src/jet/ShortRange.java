package jet;

public final class ShortRange implements Range<Short>, ShortIterable, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(ShortRange.class, false);

    private final short start;
    private final int count;

    public ShortRange(short startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    public ShortRange(short startValue, int count, boolean reversed) {
        this(startValue, reversed ? -count : count);
    }

    public ShortRange(short startValue, int count, boolean reversed, int defaultMask) {
        this(startValue, reversed ? -count : count, (defaultMask & 4) == 0);
    }

    @Override
    public boolean contains(Short item) {
        if (item == null) return false;
        if (count >= 0) {
            return item >= start && item < start + count;
        }
        return item <= start && item > start + count;
    }

    public boolean getIsReversed() {
        return count < 0;
    }

    public short getStart() {
        return start;
    }

    public short getEnd() {
        return (short) (count < 0 ? start + count + 1: start+count-1);
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    public ShortRange minus() {
        return new ShortRange(getEnd(), -count);
    }

    @Override
    public ShortIterator iterator() {
        return new MyIterator(start, count);
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }

    @Override
    public JetObject getOuterObject() {
        return null;
    }

    public static ShortRange count(int length) {
        return new ShortRange((byte) 0, length);
    }

    public static ShortRange rangeTo(short from, short to) {
        if(from > to) {
            return new ShortRange(to, from-to+1, true);
        }
        else {
            return new ShortRange(from, to-from+1);
        }
    }

    private static class MyIterator extends ShortIterator {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private short cur;
        private int count;

        private final boolean reversed;

        public MyIterator(short startValue, int count) {
            cur = startValue;
            reversed = count < 0;
            this.count = reversed ? -count : count;
        }

        @Override
        public boolean getHasNext() {
            return count > 0;
        }

        @Override
        public short nextShort() {
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

        @Override
        public JetObject getOuterObject() {
            return null;
        }
    }
}
