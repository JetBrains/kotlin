package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public class DefaultJetObject implements JetObject {
    private TypeInfo<?> typeInfo;

    protected DefaultJetObject() {
    }

    protected DefaultJetObject(TypeInfo<?> typeInfo) {
        this.typeInfo = typeInfo;
    }

    protected final void $setTypeInfo(TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    @Override
    public final TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }
}
