package jet;

/**
 * @author alex.tkachman
 */
public interface FloatIterable extends Iterable<Float> {
    @Override
    FloatIterator iterator();
}
