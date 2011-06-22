/*
 * @author max
 */
package jet;

public abstract  class Function0<R> {
    public abstract R invoke();

    @Override
    public String toString() {
        return "{() : R}";
    }
}
