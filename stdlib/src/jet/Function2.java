/*
 * @author max
 */
package jet;

public abstract  class Function2<D1, D2, R> {
    public abstract R invoke(D1 d1, D2 d2);

    @Override
    public String toString() {
        return "{(d1: D1, d2: D2) : R}";
    }
}
