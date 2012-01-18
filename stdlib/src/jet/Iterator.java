package jet;

import jet.runtime.typeinfo.JetMethod;

public interface Iterator<T> extends JetObject {
    @JetMethod(kind = JetMethod.KIND_PROPERTY)
    boolean getHasNext();
    T next ();
}
