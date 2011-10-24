package jet;

public interface Iterator<T> extends JetObject {
    boolean getHasNext();
    T next ();
}
