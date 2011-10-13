/*
 * @author max
 */
package jet;

import jet.typeinfo.TypeInfo;

public abstract  class ExtensionFunction1<E, D, R>  extends DefaultJetObject{
    protected ExtensionFunction1(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(E receiver, D d);

    @Override
    public String toString() {
        return "{E.(d: D) : R}";
    }
}
