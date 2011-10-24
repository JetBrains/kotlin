/*
 * @author alex.tkachman
 */
package jet;

import jet.typeinfo.TypeInfo;
public abstract class Function2<D1, D2, R> extends DefaultJetObject {
    protected Function2(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(D1 d1, D2 d2);

    @Override
    public String toString() {
      return "{(d1: D1, d2: D2) : R)}";
    }
}

