package jet;

/**
 * @author alex.tkachman
 */
public interface DoubleIterable extends Iterable<Double> {
    @Override
    DoubleIterator iterator();
}
