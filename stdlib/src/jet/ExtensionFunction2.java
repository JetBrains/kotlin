/*
 * @author max
 */
package jet;

public abstract  class ExtensionFunction2<E, D1, D2, R> {
    public abstract R invoke(E receiver, D1 d1, D2 d2);

    @Override
    public String toString() {
        return "{E.(d1: D1, d2: D2) : R}";
    }
}
