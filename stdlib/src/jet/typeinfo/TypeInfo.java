package jet.typeinfo;

import jet.JetObject;
import jet.Tuple0;
import jet.typeinfo.internal.TypeInfoImpl;
import jet.typeinfo.internal.TypeInfoProjectionImpl;

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
    public static final TypeInfo<Boolean> BOOLEAN_TYPE_INFO = getTypeInfo(Boolean.class, false);
    public static final TypeInfo<Float> FLOAT_TYPE_INFO = getTypeInfo(Float.class, false);
    public static final TypeInfo<Double> DOUBLE_TYPE_INFO = getTypeInfo(Double.class, false);

    public static final TypeInfo<byte[]> BYTE_ARRAY_TYPE_INFO = getTypeInfo(byte[].class, false);
    public static final TypeInfo<short[]> SHORT_ARRAY_TYPE_INFO = getTypeInfo(short[].class, false);
    public static final TypeInfo<int[]> INT_ARRAY_TYPE_INFO = getTypeInfo(int[].class, false);
    public static final TypeInfo<long[]> LONG_ARRAY_TYPE_INFO = getTypeInfo(long[].class, false);
    public static final TypeInfo<char[]> CHAR_ARRAY_TYPE_INFO = getTypeInfo(char[].class, false);
    public static final TypeInfo<boolean[]> BOOL_ARRAY_TYPE_INFO = getTypeInfo(boolean[].class, false);
    public static final TypeInfo<float[]> FLOAT_ARRAY_TYPE_INFO = getTypeInfo(float[].class, false);
    public static final TypeInfo<double[]> DOUBLE_ARRAY_TYPE_INFO = getTypeInfo(double[].class, false);

    public static final TypeInfo<String> STRING_TYPE_INFO = getTypeInfo(String.class, false);
    public static final TypeInfo<Tuple0> TUPLE0_TYPE_INFO = getTypeInfo(Tuple0.class, false);

    public static final TypeInfo<Byte> NULLABLE_BYTE_TYPE_INFO = getTypeInfo(Byte.class, true);
    public static final TypeInfo<Short> NULLABLE_SHORT_TYPE_INFO = getTypeInfo(Short.class, true);
    public static final TypeInfo<Integer> NULLABLE_INT_TYPE_INFO = getTypeInfo(Integer.class, true);
    public static final TypeInfo<Long> NULLABLE_LONG_TYPE_INFO = getTypeInfo(Long.class, true);
    public static final TypeInfo<Character> NULLABLE_CHAR_TYPE_INFO = getTypeInfo(Character.class, true);
    public static final TypeInfo<Boolean> NULLABLE_BOOL_TYPE_INFO = getTypeInfo(Boolean.class, true);
    public static final TypeInfo<Float> NULLABLE_FLOAT_TYPE_INFO = getTypeInfo(Float.class, true);
    public static final TypeInfo<Double> NULLABLE_DOUBLE_TYPE_INFO = getTypeInfo(Double.class, true);
    public static final TypeInfo<String> NULLABLE_STRING_TYPE_INFO = getTypeInfo(String.class, true);
    public static final TypeInfo<Tuple0> NULLABLE_TUPLE0_TYPE_INFO = getTypeInfo(Tuple0.class, true);
    
    public abstract Object [] newArray(int length);

    public static <T> TypeInfoProjection invariantProjection(final TypeInfo<T> typeInfo) {
        return (TypeInfoProjection) typeInfo;
    }

    public static <T> TypeInfoProjection inProjection(TypeInfo<T> typeInfo) {
        return new TypeInfoProjectionImpl(typeInfo) {
//            @NotNull
            @Override
            public TypeInfoVariance getVariance() {
                return TypeInfoVariance.IN;
            }
        };
    }

    public static <T> TypeInfoProjection outProjection(TypeInfo<T> typeInfo) {
        return new TypeInfoProjectionImpl(typeInfo) {
//            @NotNull
            @Override
            public TypeInfoVariance getVariance() {
                return TypeInfoVariance.OUT;
            }
        };
    }

    private static <T> TypeInfoProjection projection(TypeInfo<T> typeInfo, TypeInfoVariance variance) {
        switch (variance) {
            case IN: return inProjection(typeInfo);
            case OUT: return outProjection(typeInfo);
            case INVARIANT: return invariantProjection(typeInfo);
            default: throw new IllegalStateException();
        }
    }

    public static <T> TypeInfo<T> getTypeInfo(Class<T> klazz, boolean nullable) {
        return new TypeInfoImpl<T>(klazz, nullable);
    }

    public static <T> TypeInfo<T> getTypeInfo(Class<T> klazz, boolean nullable, TypeInfoProjection[] projections) {
        return new TypeInfoImpl<T>(klazz, nullable, projections);
    }

    public abstract Class<T> getJavaClass();
    
    public abstract Object getClassObject();

    public abstract boolean isInstance(Object obj);

    public abstract int getProjectionCount();

    public abstract TypeInfoProjection getProjection(int index);

    public abstract TypeInfo getArgumentType(Class klass, int index);

}
