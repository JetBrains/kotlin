package jet.typeinfo;

import jet.JetObject;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * @author abreslav
 * @author yole
 * @author alex.tkachman
 */
public abstract class TypeInfo<T> implements JetObject {
    public static final TypeInfo<Byte> BYTE_TYPE_INFO = getTypeInfo(Byte.class, false);
    public static final TypeInfo<Short> SHORT_TYPE_INFO = getTypeInfo(Short.class, false);
    public static final TypeInfo<Integer> INT_TYPE_INFO = getTypeInfo(Integer.class, false);
    public static final TypeInfo<Long> LONG_TYPE_INFO = getTypeInfo(Long.class, false);
    public static final TypeInfo<Character> CHAR_TYPE_INFO = getTypeInfo(Character.class, false);
    public static final TypeInfo<Boolean> BOOL_TYPE_INFO = getTypeInfo(Boolean.class, false);
    public static final TypeInfo<Float> FLOAT_TYPE_INFO = getTypeInfo(Float.class, false);
    public static final TypeInfo<Double> DOUBLE_TYPE_INFO = getTypeInfo(Double.class, false);

    private TypeInfo<?> typeInfo;
    private final Class<T> theClass;
    private final boolean nullable;
    private final TypeInfoProjection[] projections;

    private TypeInfo(Class<T> theClass, boolean nullable) {
        this.theClass = theClass;
        this.nullable = nullable;
        this.projections = null;
    }

    private TypeInfo(Class<T> theClass, boolean nullable, TypeInfoProjection[] projections) {
        this.theClass = theClass;
        this.nullable = nullable;
        this.projections = projections;
    }

    public static <T> TypeInfoProjection invariantProjection(final TypeInfo<T> typeInfo) {
        return (TypeInfoImpl) typeInfo;
    }

    public static <T> TypeInfoProjection inProjection(TypeInfo<T> typeInfo) {
        return new TypeInfoProjection.TypeInfoProjectionImpl(typeInfo) {
            @NotNull
            @Override
            public TypeInfoVariance getVariance() {
                return TypeInfoVariance.IN_VARIANCE;
            }
        };
    }

    public static <T> TypeInfoProjection outProjection(TypeInfo<T> typeInfo) {
        return new TypeInfoProjection.TypeInfoProjectionImpl(typeInfo) {
            @NotNull
            @Override
            public TypeInfoVariance getVariance() {
                return TypeInfoVariance.OUT_VARIANCE;
            }
        };
    }

    public static <T> TypeInfo<T> getTypeInfo(Class<T> klazz, boolean nullable) {
        return new TypeInfoImpl<T>(klazz, nullable);
    }

    public static <T> TypeInfo<T> getTypeInfo(Class<T> klazz, boolean nullable, TypeInfoProjection[] projections) {
        return new TypeInfoImpl<T>(klazz, nullable, projections);
    }

    public final Object getClassObject() {
        try {
            final Class implClass = theClass.getClassLoader().loadClass(theClass.getCanonicalName() + "$$Impl");
            final Field classobj = implClass.getField("$classobj");
            return classobj.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    public final boolean isInstance(Object obj) {
        if (obj instanceof JetObject) {
            return ((JetObject) obj).getTypeInfo().isSubtypeOf(this);
        }
        if (obj == null)
            return nullable;

        return theClass.isAssignableFrom(obj.getClass());  // TODO
    }

    public final boolean isSubtypeOf(TypeInfo<?> superType) {
        if (!superType.theClass.isAssignableFrom(theClass)) {
            return false;
        }
        if (nullable && !superType.nullable) {
            return false;
        }
        if (projections != null) {
            if (superType.projections == null || superType.projections.length != projections.length) {
                throw new IllegalArgumentException("inconsistent type infos for the same class");
            }
            for (int i = 0; i < projections.length; i++) {
                // TODO handle variance here
                if (!projections[i].equals(superType.projections[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    public final TypeInfoProjection getProjection(int index) {
        return projections[index];
    }

    public final TypeInfo getArgumentType(int index) {
        return projections[index].getType();
    }

    @Override
    public final TypeInfo<?> getTypeInfo() {
        if (typeInfo == null) {
            // TODO: Implementation must be lazy, otherwise the result would be of an infinite size
            throw new UnsupportedOperationException(); // TODO
        }
        return typeInfo;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeInfo typeInfo = (TypeInfo) o;

        if (!theClass.equals(typeInfo.theClass)) return false;
        if (nullable != typeInfo.nullable) return false;
        if (!Arrays.equals(projections, typeInfo.projections)) return false;

        return true;
    }

    @Override
    public final int hashCode() {
        return 31 * theClass.hashCode() + (projections != null ? Arrays.hashCode(projections) : 0);
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder().append(theClass.getName());
        if (projections != null && projections.length != 0) {
            sb.append("<");
            for (int i = 0; i != projections.length - 1; ++i) {
                sb.append(projections[i].toString()).append(",");
            }
            sb.append(projections[projections.length - 1].toString()).append(">");
        }
        if (nullable)
            sb.append("?");
        return sb.toString();
    }

    private static class TypeInfoImpl<T> extends TypeInfo<T> implements TypeInfoProjection {
        TypeInfoImpl(Class<T> klazz, boolean nullable) {
            super(klazz, nullable);
        }

        TypeInfoImpl(Class<T> klazz, boolean nullable, TypeInfoProjection[] projections) {
            super(klazz, nullable, projections);
        }

        @NotNull
        @Override
        public TypeInfoVariance getVariance() {
            return TypeInfoVariance.INVARIANT;
        }

        @NotNull
        @Override
        public TypeInfo getType() {
            return this;
        }
    }
}
