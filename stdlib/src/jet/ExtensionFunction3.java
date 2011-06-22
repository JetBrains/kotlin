/*
 * @author max
 */
package jet;

public abstract  class ExtensionFunction3<E, D1, D2, D3, R> {
    public abstract R invoke(E receiver, D1 d1, D2 d2, D3 d3);

    @Override
    public String toString() {
        return "{E.(d1: D1, d2: D2, d3: D3) : R}";
    }
}
