package jet;

/**
 * @author alex.tkachman
 */
public abstract class CharIterator implements Iterator<Character> {
    public final Character next() {
        return nextChar();
    }

    public abstract char nextChar();
}
