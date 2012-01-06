package jet;

public final class DoubleRange implements Range<Double>, JetObject {
    private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(DoubleRange.class, false);

    private final double start;
    private final double size;

    public DoubleRange(double startValue, double size) {
        this.start = startValue;
        this.size = size;
    }

    @Override
    public boolean contains(Double item) {
        if (item == null) return false;
        if (size >= 0) {
            return item >= start && item < start + size;
        }
        return item <= start && item > start + size;
    }

    public DoubleIterator step(double step) {
        if(step < 0)
            return new MyIterator(getEnd(), -size, -step);
        else
            return new MyIterator(start, size, step);
    }

    public boolean getIsReversed() {
        return size < 0;
    }

    public double  getStart() {
        return start;
    }

    public double  getEnd() {
        return size < 0 ? start + size: start + size;
    }

    public double getSize() {
        return size < 0 ? -size : size;
    }

    public DoubleRange minus() {
        return new DoubleRange(getEnd(), -size);
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }

    @Override
    public JetObject getOuterObject() {
        return null;
    }

    public static DoubleRange count(int length) {
        return new DoubleRange(0, length);
    }

    private static class MyIterator extends DoubleIterator {
        private final static TypeInfo typeInfo = TypeInfo.getTypeInfo(MyIterator.class, false);

        private double cur;
        private double step;
        private final double end;

        private final boolean reversed;

        public MyIterator(double startValue, double size, double step) {
            cur = startValue;
            this.step = step;
            if(size < 0) {
                reversed = true;
                end = startValue-size;
                startValue -= size;
            }
            else {
                reversed = false;
                this.end = startValue + size;
            }
        }

        @Override
        public boolean getHasNext() {
            if(reversed)
                return cur >= end;
            else
                return cur <= end;
        }

        @Override
        public double nextDouble() {
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
