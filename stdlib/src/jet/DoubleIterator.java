package jet;

/**
 * @author alex.tkachman
 */
public abstract class DoubleIterator implements Iterator<Double> {
    public final Double next() {
        return nextDouble();
    }

    public abstract double nextDouble();
}
