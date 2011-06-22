/*
 * @author max
 */
package jet;

public abstract  class Function1<D, R> {
    public abstract R invoke(D d);

    @Override
    public String toString() {
        return "{(d: D) : R}";
    }
}
