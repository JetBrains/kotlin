package jet;

public interface Range<T extends Comparable<T>> {
    boolean contains(T item);
}
