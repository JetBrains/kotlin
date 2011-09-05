package jet;

public interface Iterator<T> extends JetObject {
    boolean hasNext();
    T next ();
}
