/*
 * @author max
 */
package jet;

public abstract  class ExtensionFunction1<E, D, R> {
    public abstract R invoke(E receiver, D d);

    @Override
    public String toString() {
        return "{E.(d: D) : R}";
    }
}
