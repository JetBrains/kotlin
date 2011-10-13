/*
 * @author max
 */
package jet;

import jet.typeinfo.TypeInfo;

public abstract  class Function0<R> extends DefaultJetObject {
    protected Function0(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke();

    @Override
    public String toString() {
        return "{() : R}";
    }
}
