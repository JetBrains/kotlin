package jet;

/**
 * @author alex.tkachman
 */
public abstract class ByteIterator implements Iterator<Byte> {
    public final Byte next() {
        return nextByte();
    }

    public abstract byte nextByte();
}
