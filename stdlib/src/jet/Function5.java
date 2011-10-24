/*
 * @author alex.tkachman
 */
package jet;

import jet.typeinfo.TypeInfo;
public abstract class Function5<D1, D2, D3, D4, D5, R> extends DefaultJetObject {
    protected Function5(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(D1 d1, D2 d2, D3 d3, D4 d4, D5 d5);

    @Override
    public String toString() {
      return "{(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5) : R)}";
    }
}

