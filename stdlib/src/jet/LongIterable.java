package jet;

/**
 * @author alex.tkachman
 */
public interface LongIterable extends Iterable<Long> {
    @Override
    LongIterator iterator();
}
