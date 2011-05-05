package jet.typeinfo;

import jet.JetObject;

/**
 * @author abreslav
 */
public class TypeInfo<T> implements JetObject {

    private TypeInfo<?> typeInfo;
    private final TypeInfo<?> typeArgument;

    private TypeInfo(TypeInfo<?> typeArgument) {
        this.typeArgument = typeArgument;
    }

    public boolean isInstance(Object obj) {
        throw new UnsupportedOperationException(); // TODO
    }

    public boolean isSubtypeOf(TypeInfo<?> other) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        if (typeInfo == null) {
            // TODO: Implementation must be lazy, otherwise the result would be of an infinite size
            throw new UnsupportedOperationException(); // TODO
        }
        return typeInfo;
    }
}
