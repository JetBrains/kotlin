package jet;

/**
 * @author alex.tkachman
 */
public abstract class IntIterator implements Iterator<Integer> {
    public final Integer next() {
        return nextInt();
    }

    public abstract int nextInt();
}
