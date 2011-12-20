package jet;

/**
 * @author alex.tkachman
 */
public interface IntIterable extends Iterable<Integer> {
    @Override
    IntIterator iterator();
}
