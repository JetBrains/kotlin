/*
 * @author alex.tkachman
 */
package jet;

import jet.typeinfo.TypeInfo;
public abstract class Function1<D1, R> extends DefaultJetObject {
    protected Function1(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(D1 d1);

    @Override
    public String toString() {
      return "{(d1: D1) : R)}";
    }
}

