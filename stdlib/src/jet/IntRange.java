package jet;

public class IntRange implements Range<Integer> {
    private final int startValue;
    private final int endValue;

    public IntRange(int startValue, int endValue) {
        this.startValue = startValue;
        this.endValue = endValue;
    }

    @Override
    public boolean contains(Integer item) {
        if (item == null) return false;
        if (startValue <= endValue) {
            return item >= startValue && item <= endValue;
        }
        return item <= startValue && item >= endValue;
    }

    public int getStartValue() {
        return startValue;
    }

    public int getEndValue() {
        return endValue;
    }
}
