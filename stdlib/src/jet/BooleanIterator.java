package jet;

/**
 * @author alex.tkachman
 */
public abstract class BooleanIterator implements Iterator<Boolean> {
    public final Boolean next() {
        return nextBoolean();
    }

    public abstract boolean nextBoolean();
}
