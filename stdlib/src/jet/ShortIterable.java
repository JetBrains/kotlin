package jet;

/**
 * @author alex.tkachman
 */
public interface ShortIterable extends Iterable<Short> {
    @Override
    ShortIterator iterator();
}
