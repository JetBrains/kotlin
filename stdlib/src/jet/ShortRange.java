package jet;

public final class ShortRange implements Range<Short>, ShortIterable, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(ShortRange.class, false);

    private final short start;
    private final int count;

    public static final ShortRange empty = new ShortRange((short) 0,0);

    public ShortRange(short startValue, int count) {
        this.start = startValue;
        this.count = count;
    }

    public ShortIterator step(int step) {
        if(step < 0)
            return new MyIterator(getEnd(), -count, -step);
        else
            return new MyIterator(start, count, step);
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
        return (short) (count < 0 ? start + count + 1: count == 0 ? 0 : start+count-1);
    }

    public int getSize() {
        return count < 0 ? -count : count;
    }

    public ShortRange minus() {
        return new ShortRange(getEnd(), -count);
    }

    @Override
    public ShortIterator iterator() {
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

    public static ShortRange count(int length) {
        return new ShortRange((byte) 0, length);
    }

    private static class MyIterator extends ShortIterator {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private short cur;
        private int step;
        private int count;

        private final boolean reversed;

        public MyIterator(short startValue, int count, int step) {
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
        public short nextShort() {
            count -= step;
            if(reversed) {
                cur -= step;
                return (short) (cur + step);
            }
            else {
                cur += step;
                return (short) (cur - step);
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
