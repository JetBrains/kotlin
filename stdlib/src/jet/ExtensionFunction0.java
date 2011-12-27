/*
 * @author max
 */
package jet;

public abstract  class ExtensionFunction0<E, R>  extends DefaultJetObject {
    protected ExtensionFunction0(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(E receiver);

    @Override
    public String toString() {
        return "{E.() : R}";
    }
}
