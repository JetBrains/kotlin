/*
 * @author max
 */
package jet;

public abstract  class ExtensionFunction0<E, R> {
    public abstract R invoke(E receiver);

    @Override
    public String toString() {
        return "{E.() : R}";
    }
}
