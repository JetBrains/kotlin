package jet;

/**
 * @author alex.tkachman
 */
public interface BooleanIterable extends Iterable<Boolean> {
    @Override
    BooleanIterator iterator();
}
