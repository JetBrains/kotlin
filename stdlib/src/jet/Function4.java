/*
 * @author alex.tkachman
 */
package jet;

import jet.typeinfo.TypeInfo;
public abstract class Function4<D1, D2, D3, D4, R> extends DefaultJetObject {
    protected Function4(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(D1 d1, D2 d2, D3 d3, D4 d4);

    @Override
    public String toString() {
      return "{(d1: D1, d2: D2, d3: D3, d4: D4) : R)}";
    }
}

