package jet.typeinfo;

import jet.JetObject;

import java.util.Arrays;

/**
 * @author abreslav
 * @author yole
 */
public class TypeInfo<T> implements JetObject {
    private TypeInfo<?> typeInfo;
    private final Class<T> theClass;
    private final boolean nullable;
    private final TypeInfo[] typeParameters;

    public TypeInfo(Class<T> theClass, boolean nullable) {
        this.theClass = theClass;
        this.nullable = nullable;
        this.typeParameters = null;
    }

    public TypeInfo(Class<T> theClass, boolean nullable, TypeInfo[] typeParameters) {
        this.theClass = theClass;
        this.nullable = nullable;
        this.typeParameters = typeParameters;
    }

    public boolean isInstance(Object obj) {
        if (obj instanceof JetObject) {
            return isSubtypeOf(((JetObject) obj).getTypeInfo());
        }
        throw new UnsupportedOperationException(); // TODO
    }

    public boolean isSubtypeOf(TypeInfo<?> other) {
        if (!theClass.isAssignableFrom(other.theClass)) {
            return false;
        }
        if (nullable != other.nullable) {
            return false;
        }
        if (typeParameters != null) {
            if (other.typeParameters == null || other.typeParameters.length != typeParameters.length) {
                throw new IllegalArgumentException("inconsistent type infos for the same class");
            }
            for (int i = 0; i < typeParameters.length; i++) {
                // TODO handle variance here
                if (!typeParameters [i].equals(other.typeParameters [i])) {
                    return false;
                }
            }
        }
        return true;
    }

    public TypeInfo getTypeParameter(int index) {
        return typeParameters[index];
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        if (typeInfo == null) {
            // TODO: Implementation must be lazy, otherwise the result would be of an infinite size
            throw new UnsupportedOperationException(); // TODO
        }
        return typeInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeInfo typeInfo = (TypeInfo) o;

        if (!theClass.equals(typeInfo.theClass)) return false;
        if (nullable != typeInfo.nullable) return false;
        if (!Arrays.equals(typeParameters, typeInfo.typeParameters)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = theClass.hashCode();
        result = 31 * result + (typeParameters != null ? Arrays.hashCode(typeParameters) : 0);
        return result;
    }

    public static final TypeInfo<Byte> BYTE_TYPE_INFO = new TypeInfo<Byte>(Byte.class, false);
    public static final TypeInfo<Short> SHORT_TYPE_INFO = new TypeInfo<Short>(Short.class, false);
    public static final TypeInfo<Integer> INT_TYPE_INFO = new TypeInfo<Integer>(Integer.class, false);
    public static final TypeInfo<Long> LONG_TYPE_INFO = new TypeInfo<Long>(Long.class, false);
    public static final TypeInfo<Character> CHAR_TYPE_INFO = new TypeInfo<Character>(Character.class, false);
    public static final TypeInfo<Boolean> BOOL_TYPE_INFO = new TypeInfo<Boolean>(Boolean.class, false);
    public static final TypeInfo<Float> FLOAT_TYPE_INFO = new TypeInfo<Float>(Float.class, false);
    public static final TypeInfo<Double> DOUBLE_TYPE_INFO = new TypeInfo<Double>(Double.class, false);
}
