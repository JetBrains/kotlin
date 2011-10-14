package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public class DefaultJetObject implements JetObject {
    private TypeInfo<?> typeInfo;

    protected DefaultJetObject(TypeInfo<?> typeInfo) {
        this.typeInfo = typeInfo;
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }
}
