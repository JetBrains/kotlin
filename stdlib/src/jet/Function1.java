/*
 * @author max
 */
package jet;

import jet.typeinfo.TypeInfo;

public abstract  class Function1<D, R>  extends DefaultJetObject {
    protected Function1(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(D d);

    @Override
    public String toString() {
        return "{(d: D) : R}";
    }
}
