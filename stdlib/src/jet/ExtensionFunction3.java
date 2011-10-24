/*
 * @author alex.tkachman
 */
package jet;

import jet.typeinfo.TypeInfo;
public abstract class ExtensionFunction3<E, D1, D2, D3, R> extends DefaultJetObject {
    protected ExtensionFunction3(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(E receiver, D1 d1, D2 d2, D3 d3);

    @Override
    public String toString() {
      return "{E.(d1: D1, d2: D2, d3: D3) : R)}";
    }
}

