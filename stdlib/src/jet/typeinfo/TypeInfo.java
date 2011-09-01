package jet.typeinfo;

import jet.JetObject;

import java.lang.reflect.Field;
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

    public Object getClassObject() {
        try {
            final Class implClass = theClass.getClassLoader().loadClass(theClass.getCanonicalName() + "$$Impl");
            final Field classobj = implClass.getField("$classobj");
            return classobj.get(null);
        }
        catch(Exception e) {
            return null;
        }
    }

    public boolean isInstance(Object obj) {
        if (obj instanceof JetObject) {
            return ((JetObject) obj).getTypeInfo().isSubtypeOf(this);
        }
        if(obj == null)
            return nullable;

        return theClass.isAssignableFrom(obj.getClass());  // TODO
    }

    public boolean isSubtypeOf(TypeInfo<?> superType) {
        if (!superType.theClass.isAssignableFrom(theClass)) {
            return false;
        }
        if (nullable && !superType.nullable) {
            return false;
        }
        if (typeParameters != null) {
            if (superType.typeParameters == null || superType.typeParameters.length != typeParameters.length) {
                throw new IllegalArgumentException("inconsistent type infos for the same class");
            }
            for (int i = 0; i < typeParameters.length; i++) {
                // TODO handle variance here
                if (!typeParameters [i].equals(superType.typeParameters [i])) {
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
        return 31 * theClass.hashCode() + (typeParameters != null ? Arrays.hashCode(typeParameters) : 0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append(theClass.getName());
        if(typeParameters != null && typeParameters.length != 0) {
            sb.append("<");
            for(int i = 0; i != typeParameters.length-1; ++i) {
                sb.append(typeParameters[i].toString()).append(",");
            }
            sb.append(typeParameters[typeParameters.length - 1].toString()).append(">");
        }
        if(nullable)
            sb.append("?");
        return sb.toString();
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
