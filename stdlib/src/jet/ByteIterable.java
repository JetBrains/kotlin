package jet;

/**
 * @author alex.tkachman
 */
public interface ByteIterable extends Iterable<Byte> {
    @Override
    ByteIterator iterator();
}
