package jet;

/**
 * @author alex.tkachman
 */
public interface CharIterable extends Iterable<Character> {
    @Override
    CharIterator iterator();
}
