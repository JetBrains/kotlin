/*
 * @author alex.tkachman
 */
package jet;

import jet.typeinfo.TypeInfo;
public abstract class ExtensionFunction4<E, D1, D2, D3, D4, R> extends DefaultJetObject {
    protected ExtensionFunction4(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(E receiver, D1 d1, D2 d2, D3 d3, D4 d4);

    @Override
    public String toString() {
      return "{E.(d1: D1, d2: D2, d3: D3, d4: D4) : R)}";
    }
}

