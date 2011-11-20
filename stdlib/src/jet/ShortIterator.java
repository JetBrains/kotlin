package jet;

/**
 * @author alex.tkachman
 */
public abstract class ShortIterator implements Iterator<Short> {
    public final Short next() {
        return nextShort();
    }

    public abstract short nextShort();
}
