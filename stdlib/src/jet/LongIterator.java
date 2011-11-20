package jet;

/**
 * @author alex.tkachman
 */
public abstract class LongIterator implements Iterator<Long> {
    public final Long next() {
        return nextLong();
    }

    public abstract long nextLong();
}
