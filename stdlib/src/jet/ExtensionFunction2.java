/*
 * @author alex.tkachman
 */
package jet;

import jet.typeinfo.TypeInfo;
public abstract class ExtensionFunction2<E, D1, D2, R> extends DefaultJetObject {
    protected ExtensionFunction2(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(E receiver, D1 d1, D2 d2);

    @Override
    public String toString() {
      return "{E.(d1: D1, d2: D2) : R)}";
    }
}

