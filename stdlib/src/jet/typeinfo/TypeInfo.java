package jet.typeinfo;

import jet.JetObject;
import jet.Tuple0;

import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author abreslav
 * @author yole
 * @author alex.tkachman
 */
public abstract class TypeInfo<T> implements JetObject {
    private final static TypeInfoProjection[] EMPTY = new TypeInfoProjection[0];

    public static final TypeInfo<Byte> BYTE_TYPE_INFO = getTypeInfo(Byte.class, false);
    public static final TypeInfo<Short> SHORT_TYPE_INFO = getTypeInfo(Short.class, false);
    public static final TypeInfo<Integer> INT_TYPE_INFO = getTypeInfo(Integer.class, false);
    public static final TypeInfo<Long> LONG_TYPE_INFO = getTypeInfo(Long.class, false);
    public static final TypeInfo<Character> CHAR_TYPE_INFO = getTypeInfo(Character.class, false);
    public static final TypeInfo<Boolean> BOOL_TYPE_INFO = getTypeInfo(Boolean.class, false);
    public static final TypeInfo<Float> FLOAT_TYPE_INFO = getTypeInfo(Float.class, false);
    public static final TypeInfo<Double> DOUBLE_TYPE_INFO = getTypeInfo(Double.class, false);
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

    private TypeInfo<?> typeInfo;
    private final Signature signature;
    private final boolean nullable;
    private final TypeInfoProjection[] projections;

    private TypeInfo(Class<T> theClass, boolean nullable) {
        this(theClass, nullable, EMPTY);
    }

    private TypeInfo(Class<T> theClass, boolean nullable, TypeInfoProjection[] projections) {
        this.signature = Parser.parse(theClass);
        this.nullable = nullable;
        this.projections = projections;
        if(signature.variables.size() != projections.length)
            throw new IllegalStateException("Wrong signature " + theClass.getName());
    }

    public static <T> TypeInfoProjection invariantProjection(final TypeInfo<T> typeInfo) {
        return (TypeInfoImpl) typeInfo;
    }

    public static <T> TypeInfoProjection inProjection(TypeInfo<T> typeInfo) {
        return new TypeInfoProjection.TypeInfoProjectionImpl(typeInfo) {
//            @NotNull
            @Override
            public TypeInfoVariance getVariance() {
                return TypeInfoVariance.IN_VARIANCE;
            }
        };
    }

    public static <T> TypeInfoProjection outProjection(TypeInfo<T> typeInfo) {
        return new TypeInfoProjection.TypeInfoProjectionImpl(typeInfo) {
//            @NotNull
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
            final Class implClass = signature.klazz.getClassLoader().loadClass(signature.klazz.getCanonicalName());
            final Field classobj = implClass.getField("$classobj");
            return classobj.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    public final boolean isInstance(Object obj) {
        if (obj == null) return nullable;

        if (obj instanceof JetObject) {
            return ((JetObject) obj).getTypeInfo().isSubtypeOf(this);
        }

        return signature.klazz.isAssignableFrom(obj.getClass());  // TODO
    }

    public final boolean isSubtypeOf(TypeInfo<?> superType) {
        if (nullable && !superType.nullable) {
            return false;
        }
        if (!superType.signature.klazz.isAssignableFrom(signature.klazz)) {
            return false;
        }
        if (superType.projections == null || superType.projections.length != projections.length) {
            throw new IllegalArgumentException("inconsistent type infos for the same class");
        }
        for (int i = 0; i < projections.length; i++) {
            // TODO handle variance here
            if (!projections[i].equals(superType.projections[i])) {
                return false;
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

        if (!signature.klazz.equals(typeInfo.signature.klazz)) return false;
        if (nullable != typeInfo.nullable) return false;
        if (!Arrays.equals(projections, typeInfo.projections)) return false;

        return true;
    }

    @Override
    public final int hashCode() {
        return 31 * signature.klazz.hashCode() + Arrays.hashCode(projections);
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder().append(signature.klazz.getName());
        if (projections.length != 0) {
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

//        @NotNull
        @Override
        public TypeInfoVariance getVariance() {
            return TypeInfoVariance.INVARIANT;
        }

//        @NotNull
        @Override
        public TypeInfo getType() {
            return this;
        }
    }

    public static class Signature {
        final Class klazz;
        final List<Var> variables;
        final List<Type> superTypes;
        
        final HashMap<Class,Signature> superSignatures = new HashMap<Class,Signature>();

        public Signature(Class klazz, List<Var> variables, List<Type> superTypes) {
            this.klazz = klazz;
            this.superTypes = superTypes;
            this.variables = variables;
            
            for(Type superType : superTypes) {
                if(superType instanceof TypeReal) {
                    TypeReal type = (TypeReal) superType;
                    Signature parse = Parser.parse(type.klazz);
                    superSignatures.put(type.klazz, parse);
                    for(Map.Entry<Class,Signature> entry : parse.superSignatures.entrySet()) {
                        superSignatures.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    public static class Var {
        final String name;
        final TypeInfoVariance variance;

        public Var(String name, TypeInfoVariance variance) {
            this.name = name;
            this.variance = variance;
        }
    }

    public abstract static class Type{
        final boolean nullable;

        protected Type(boolean nullable) {
            this.nullable = nullable;
        }
    }

    public static class TypeVar extends Type {
        final int varIndex;

        public TypeVar(boolean nullable, int varIndex) {
            super(nullable);
            this.varIndex = varIndex;
        }
    }

    public static class TypeProj {
        public final TypeInfoVariance variance;
        public final Type type;

        public TypeProj(TypeInfoVariance variance, Type type) {
            this.variance = variance;
            this.type = type;
        }
    }

    public static class TypeReal extends Type {
        public final Class klazz;
        public final List<TypeProj> params;

        public TypeReal(Class klazz, boolean nullable, List<TypeProj> params) {
            super(nullable);
            this.params = params;
            this.klazz = klazz;
        }
    }

    public static class Parser {
        static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        static final WeakHashMap<Class,Signature> map = new WeakHashMap<Class,Signature>();

        int cur;
        final char [] string;

        Map<String,Integer> variables;

        final ClassLoader classLoader;

        Parser(String string, ClassLoader classLoader) {
            this.classLoader = classLoader;
            this.string = string.toCharArray();
        }

        public static Signature parse(Class klass) {
            lock.readLock().lock();
            Signature sig = map.get(klass);
            lock.readLock().unlock();
            if (sig == null) {
                lock.writeLock().lock();

                sig = map.get(klass);
                if (sig == null) {
                    sig = internalParse(klass);
                    map.put(klass, sig);
                }

                lock.writeLock().unlock();
            }
            return sig;
        }

        private static Signature internalParse(Class klass) {
            JetSignature annotation = (JetSignature) klass.getAnnotation(JetSignature.class);
            if(annotation != null) {
                String value = annotation.value();
                if(value != null) {
                    Parser parser = new Parser(value, klass.getClassLoader());
                    return new Signature(klass, parser.parseVars(), parser.parseTypes());
                }
            }
            return getGenericSignature(klass);
        }

        private static Signature getGenericSignature(Class klass) {
            // todo complete impl

            TypeVariable[] typeParameters = klass.getTypeParameters();
            Map<String,Integer> variables;
            List<Var> vars;
            if(typeParameters == null || typeParameters.length == 0) {
                variables = Collections.emptyMap();
                vars = Collections.emptyList();
            }
            else {
                variables = new HashMap<String, Integer>();
                vars = new LinkedList<Var>();
                for (int i = 0; i < typeParameters.length; i++) {
                    TypeVariable typeParameter = typeParameters[i];
                    variables.put(typeParameter.getName(), i);
                    vars.add(new Var(typeParameter.getName(), TypeInfoVariance.INVARIANT));
                }
            }

            List<Type> types = new LinkedList<Type>();
            java.lang.reflect.Type genericSuperclass = klass.getGenericSuperclass();
            return new Signature(klass, vars, types);
        }

        public List<Var> parseVars() {
            List<Var> list = null;
            while(cur != string.length && string[cur] == 'T') {
                if(list == null)
                    list = new LinkedList<Var>();
                list.add(parseVar());
            }
            List<Var> vars = list == null ? Collections.<Var>emptyList() : list;
            if(vars.isEmpty())
                variables = Collections.emptyMap();
            else {
                variables = new HashMap<String, Integer>();
                for(int i=0; i != list.size(); ++i) {
                    variables.put(list.get(i).name, i);
                }
            }
            return vars;
        }

        private Var parseVar() {
            TypeInfoVariance variance = parseVariance();
            String name = parseName();
            return new Var(name, variance);
        }

        private String parseName() {
            int c = ++cur; // skip 'T'
            while (string[c] != ';') {
                c++;
            }
            String name = new String(string, cur, c - cur);
            cur = c+1;
            return name;
        }

        private TypeInfoVariance parseVariance() {
            if(string[cur] == 'i' && string[cur+1] == 'n' && string[cur+2] == ' ' ) {
                cur += 3;
                return TypeInfoVariance.IN_VARIANCE;
            }
            else if (string[cur] == 'o' && string[cur+1] == 'u' && string[cur+2] == 't' && string[cur+2] == ' ') {
                cur += 4;
                return TypeInfoVariance.OUT_VARIANCE;
            }
            else {
                return TypeInfoVariance.INVARIANT;
            }
        }

        public List<Type> parseTypes() {
            List<Type> types = null;
            while(cur != string.length) {
                if(types == null) {
                    types = new LinkedList<Type>();
                }
                types.add(parseType());
            }
            return types == null ? Collections.<Type>emptyList() : types;
        }

        private Type parseType() {
            switch (string[cur]) {
                case 'L':
                    String name = parseName();
                    Class<?> aClass;
                    try {
                        aClass = classLoader.loadClass(name.replace('/','.'));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    List<TypeProj> proj = null;
                    boolean nullable = false;
                    if(cur != string.length && string[cur] == '<') {
                        cur++;
                        while(string[cur] != '>') {
                            if(proj == null)
                                proj = new LinkedList<TypeProj>();
                            proj.add(parseProjection());
                        }
                        cur++;
                    }
                    if(cur != string.length && string[cur] == '?') {
                        cur++;
                        nullable = true;
                    }
                    return new TypeReal(aClass, nullable, proj == null ? Collections.<TypeProj>emptyList() : proj);

                case 'T':
                    return parseTypeVar();

                default:
                    throw new IllegalStateException(new String(string));
            }
        }

        private Type parseTypeVar() {
            String name = parseName();
            boolean nullable = false;
            if(string[cur] == '?') {
                nullable = true;
                cur++;
            }

            return new TypeVar(nullable, variables.get(name));
        }

        private TypeProj parseProjection() {
            TypeInfoVariance variance = parseVariance();
            Type type = parseType();
            return new TypeProj(variance, type);
        }
    }
}
