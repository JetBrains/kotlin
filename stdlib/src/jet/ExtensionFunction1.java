/*
 * @author alex.tkachman
 */
package jet;

import jet.typeinfo.TypeInfo;
public abstract class ExtensionFunction1<E, D1, R> extends DefaultJetObject {
    protected ExtensionFunction1(TypeInfo<?> typeInfo) {
        super(typeInfo);
    }

    public abstract R invoke(E receiver, D1 d1);

    @Override
    public String toString() {
      return "{E.(d1: D1) : R)}";
    }
}

