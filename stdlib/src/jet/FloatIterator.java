package jet;

/**
 * @author alex.tkachman
 */
public abstract class FloatIterator implements Iterator<Float> {
    public final Float next() {
        return nextFloat();
    }

    public abstract float nextFloat();
}
