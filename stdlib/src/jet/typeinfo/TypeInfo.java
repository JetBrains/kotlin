package jet.typeinfo;

import jet.JetObject;
import jet.Tuple0;

import java.lang.reflect.Array;
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

    public static <T> TypeInfo<T> getTypeInfo(Class<T> klazz, boolean nullable, TypeInfo outerTypeInfo) {
        return new TypeInfoImpl<T>(klazz, nullable, outerTypeInfo);
    }

    public static <T> TypeInfo<T> getTypeInfo(Class<T> klazz, boolean nullable, TypeInfo outerTypeInfo, TypeInfoProjection[] projections) {
        return new TypeInfoImpl<T>(klazz, nullable, outerTypeInfo, projections);
    }

    public static <T> TypeInfo<T> getTypeInfo(Class<T> klazz, boolean nullable, TypeInfoProjection[] projections) {
        return new TypeInfoImpl<T>(klazz, nullable, projections);
    }

    public abstract Class<T> getJavaClass();
    
    public abstract Object getClassObject();

    public abstract boolean isInstance(Object obj);

    public abstract int getProjectionCount();

    public abstract TypeInfo getOuterTypeInfo();

    public abstract TypeInfoProjection getProjection(int index);

    public abstract TypeInfo getArgumentType(Class klass, int index);
    
    protected abstract TypeInfo substitute(List<TypeInfo> myVars);

    protected abstract TypeInfo substitute(TypeInfo outer, TypeInfoProjection[] projections);

    private static class TypeInfoVar<T> extends TypeInfo<T> {
        final int varIndex;
        final Signature signature;
        final boolean nullable;

        private TypeInfoVar(Signature signature, Integer varIndex) {
            this.signature = signature;
            this.varIndex = varIndex;
            nullable = false;
        }

        public TypeInfoVar(Signature signature, boolean nullable, int varIndex) {
            this.signature = signature;
            this.nullable = nullable;
            this.varIndex = varIndex;
        }

        @Override
        public Object[] newArray(int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<T> getJavaClass() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getClassObject() {
            throw new UnsupportedOperationException("Abstract TypeInfo");
        }

        @Override
        public boolean isInstance(Object obj) {
            throw new UnsupportedOperationException("Abstract TypeInfo");
        }

        @Override
        public int getProjectionCount() {
            return 0;
        }

        @Override
        public TypeInfo getOuterTypeInfo() {
            return null;
        }

        @Override
        public TypeInfoProjection getProjection(int index) {
            throw new UnsupportedOperationException("Abstract TypeInfo");
        }

        @Override
        public TypeInfo getArgumentType(Class klass, int index) {
            throw new UnsupportedOperationException("Abstract TypeInfo");
        }

        @Override
        public TypeInfo substitute(List<TypeInfo> myVars) {
            return myVars.get(varIndex);
        }

        @Override
        protected TypeInfo substitute(TypeInfo outer, TypeInfoProjection[] projections) {
            return projections[varIndex].getType();
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            throw new UnsupportedOperationException("Abstract TypeInfo");
        }

        @Override
        public String toString() {
            return "T:" + signature.klazz.getName() + ":" + varIndex;
        }
    }

    private static class TypeInfoImpl<T> extends TypeInfo<T> implements TypeInfoProjection {
        private TypeInfo<?> typeInfo;
        private final Signature signature;
        private final boolean nullable;
        private final TypeInfoProjection[] projections;
        private final TypeInfo outerTypeInfo;

        private TypeInfoImpl(Class<T> theClass, boolean nullable) {
            this(theClass, nullable, null, EMPTY);
        }

        private TypeInfoImpl(Class<T> theClass, boolean nullable, TypeInfo outerTypeInfo) {
            this(theClass, nullable, outerTypeInfo, EMPTY);
        }

        private TypeInfoImpl(Class<T> theClass, boolean nullable, TypeInfoProjection[] projections) {
            this(theClass, nullable, null, projections);
        }
        
        private TypeInfoImpl(Class<T> theClass, boolean nullable, TypeInfo outerTypeInfo, TypeInfoProjection[] projections) {
            this.outerTypeInfo = outerTypeInfo;
            this.signature = Parser.parse(theClass);
            this.nullable = nullable;
            this.projections = projections;
            if(signature.variables.size() != projections.length)
                throw new IllegalStateException("Wrong signature " + theClass.getName());
        }

        public final TypeInfoProjection getProjection(int index) {
            return projections[index];
        }

        @Override
        public Object[] newArray(int length) {
            return (Object[]) Array.newInstance(signature.klazz, length);
        }

        @Override
        public Class<T> getJavaClass() {
            return signature.klazz;
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

        TypeInfo getSuperTypeInfo(Class klass) {
            return signature.superSignatures.get(klass);
        }

        public final TypeInfo getArgumentType(Class klass, int index) {
            if(klass == this.signature.klazz)
                return projections[index].getType();
            else {
                return getSuperTypeInfo(klass).substitute(outerTypeInfo, projections).getArgumentType(klass, index);
            }
        }

        @Override
        public TypeInfo substitute(final List<TypeInfo> myVars) {
            if(projections.length == 0)
                return new TypeInfoImpl(this.signature.klazz, nullable, EMPTY);
            else {
                TypeInfoProjection [] proj = new TypeInfoProjection[projections.length];
                for(int i = 0; i != proj.length; ++i) {
                    final int finalI = i;
                    final TypeInfo substitute = projections[finalI].getType().substitute(myVars);
                    proj[i] = new TypeInfoProjection(){

                        @Override
                        public TypeInfoVariance getVariance() {
                            return projections[finalI].getVariance();
                        }

                        @Override
                        public TypeInfo getType() {
                            return substitute;
                        }

                        @Override
                        public String toString() {
                            return getVariance().toString() + " " + substitute;
                        }
                    };
                }
                return new TypeInfoImpl(this.signature.klazz, nullable, proj);
            }
        }

        @Override
        protected TypeInfo substitute(TypeInfo outer, TypeInfoProjection[] prj) {
            if(projections.length == 0)
                return new TypeInfoImpl(signature.klazz, nullable, EMPTY);
            else {
                TypeInfoProjection [] proj = new TypeInfoProjection[projections.length];
                for(int i = 0; i != proj.length; ++i) {
                    final int finalI = i;
                    final TypeInfo substitute = projections[finalI].getType().substitute(outer, prj);
                    proj[i] = new TypeInfoProjection(){

                        @Override
                        public TypeInfoVariance getVariance() {
                            return projections[finalI].getVariance();
                        }

                        @Override
                        public TypeInfo getType() {
                            return substitute;
                        }

                        @Override
                        public String toString() {
                            return getVariance().toString() + " " + substitute;
                        }
                    };
                }
                return new TypeInfoImpl(signature.klazz, nullable, proj);
            }
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TypeInfoImpl typeInfo = (TypeInfoImpl) o;

            if (!signature.klazz.equals(typeInfo.signature.klazz)) return false;
            if (nullable != typeInfo.nullable) return false;
            if (!Arrays.equals(projections, typeInfo.projections)) return false;

            return true;
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

        @Override
        public final int hashCode() {
            return 31 * signature.klazz.hashCode() + Arrays.hashCode(projections);
        }

        public final boolean isInstance(Object obj) {
            if (obj == null) return nullable;

            if (obj instanceof JetObject) {
                return ((TypeInfoImpl)((JetObject) obj).getTypeInfo()).isSubtypeOf(this);
            }

            return signature.klazz.isAssignableFrom(obj.getClass());  // TODO
        }

        @Override
        public int getProjectionCount() {
            return projections.length;
        }

        @Override
        public TypeInfo getOuterTypeInfo() {
            return outerTypeInfo;
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

        @Override
        public final TypeInfo<?> getTypeInfo() {
            if (typeInfo == null) {
                // TODO: Implementation must be lazy, otherwise the result would be of an infinite size
                throw new UnsupportedOperationException(); // TODO
            }
            return typeInfo;
        }

        public final boolean isSubtypeOf(TypeInfoImpl<?> superType) {
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
                if (!projections[i].getType().equals(superType.projections[i].getType())) {
                    return false;
                }
            }
            return true;
        }

    }

    public static class Signature {
        final Signature outer;
        final Class klazz;

        List<TypeInfoProjection> variables;
        Map<String,Integer> varNames;
        List<TypeInfo> superTypes;
        
        final HashMap<Class,TypeInfo> superSignatures = new HashMap<Class,TypeInfo>();

        public Signature(Signature outer, Class klazz) {
            this.outer = outer;
            this.klazz = klazz;
        }

        protected void afterParse() {
            List<TypeInfo> myVars = variables == null ? Collections.<TypeInfo>emptyList() : new LinkedList<TypeInfo>();
            if(variables != null)
                for(int i = 0; i != variables.size(); ++i)
                    myVars.add(new TypeInfoVar(this, false, i));

            for(TypeInfo superType : superTypes) {
                if(superType instanceof TypeInfoImpl) {
                    TypeInfoImpl type = (TypeInfoImpl) superType;
                    Signature superSignature = Parser.parse(type.signature.klazz);

                    TypeInfo substituted = type.substitute(myVars);
                    superSignatures.put(type.signature.klazz, substituted);

                    List<TypeInfo> vars = Collections.emptyList();
                    if(superType.getProjectionCount() != 0) {
                        vars = new LinkedList<TypeInfo>();
                        for(int i=0; i != superType.getProjectionCount(); ++i) {
                            TypeInfo substitute = superType.getProjection(i).getType().substitute(myVars);
                            vars.add(substitute);
                        }
                    }

                    for(Map.Entry<Class,TypeInfo> entry : superSignature.superSignatures.entrySet()) {
                        superSignatures.put(entry.getKey(), entry.getValue().substitute(vars));
                    }
                }
            }
        }
    }

    public static class Parser {
        static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        static final WeakHashMap<Class,Signature> map = new WeakHashMap<Class,Signature>();

        int cur;
        final char [] string;

        final ClassLoader classLoader;

        Parser(String string, ClassLoader classLoader) {
            this.classLoader = classLoader;
            this.string = string.toCharArray();
        }

        public static Signature parse(Class klass) {
            if(klass == null)
                return null;

            lock.readLock().lock();
            Signature sig = map.get(klass);
            lock.readLock().unlock();
            if (sig == null) {
                lock.writeLock().lock();

                sig = map.get(klass);
                if (sig == null) {
                    sig = internalParse(klass);
                }

                lock.writeLock().unlock();
            }
            return sig;
        }

        private static Signature internalParse(Class klass) {
            if(klass.isArray() && !klass.getComponentType().isPrimitive()) {
                Signature signature = new ArraySignature(klass);
                map.put(klass, signature);
                return signature;
            }

            JetSignature annotation = (JetSignature) klass.getAnnotation(JetSignature.class);
            if(annotation != null) {
                String value = annotation.value();
                if(value != null) {
                    Parser parser = new Parser(value, klass.getClassLoader());
                    Class enclosingClass = klass.getEnclosingClass();
                    Signature signature = new Signature(parse(enclosingClass), klass);
                    map.put(klass, signature);
                    parser.parseVars(signature);
                    parser.parseTypes(signature);
                    signature.afterParse();
                    return signature;
                }
            }
            return getGenericSignature(klass);
        }

        private static Signature getGenericSignature(Class klass) {
            // todo complete impl

            java.lang.reflect.Type genericSuperclass = klass.getGenericSuperclass();
            Signature signature = new Signature(parse(klass.getEnclosingClass()), klass);

            TypeVariable[] typeParameters = klass.getTypeParameters();
            if(typeParameters == null || typeParameters.length == 0) {
                signature.varNames = Collections.emptyMap();
                signature.variables = Collections.emptyList();
            }
            else {
                signature.varNames = new HashMap<String, Integer>();
                signature.variables = new LinkedList<TypeInfoProjection>();
            }

            if (typeParameters != null && typeParameters.length != 0) {
                for (int i = 0; i < typeParameters.length; i++) {
                    TypeVariable typeParameter = typeParameters[i];
                    signature.varNames.put(typeParameter.getName(), i);
                    final TypeInfoVar typeInfoVar = new TypeInfoVar(signature, false, i);
                    signature.variables.add(new TypeInfoProjection(){

                        @Override
                        public TypeInfoVariance getVariance() {
                            return TypeInfoVariance.INVARIANT;
                        }

                        @Override
                        public TypeInfo getType() {
                            return typeInfoVar;
                        }

                        @Override
                        public String toString() {
                            return typeInfoVar.toString();
                        }
                    });
                }
            }

            return signature;
        }

        public void parseVars(Signature signature) {
            while(cur < string.length && string[cur] == 'T') {
                if(signature.variables == null) {
                    signature.variables  = new LinkedList<TypeInfoProjection>();
                    signature.varNames = new HashMap<String, Integer>();
                }
                cur++;
                signature.variables.add(parseVar(signature));
            }
            signature.variables = signature.variables == null ? Collections.<TypeInfoProjection>emptyList() : signature.variables;
            signature.varNames  = signature.varNames == null ? Collections.<String,Integer>emptyMap() : signature.varNames;
        }

        private TypeInfoProjection parseVar(Signature signature) {
            final TypeInfoVariance variance = parseVariance();
            String name = parseName();
            final TypeInfoVar typeInfoVar = new TypeInfoVar(signature, signature.variables.size());
            signature.varNames.put(name, signature.variables.size());
            return new TypeInfoProjection(){
                @Override
                public TypeInfoVariance getVariance() {
                    return variance;
                }

                @Override
                public TypeInfo getType() {
                    return typeInfoVar;
                }

                @Override
                public String toString() {
                    return typeInfoVar.toString();
                }
            };
        }

        private String parseName() {
            int c = cur;
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
            else if (string[cur] == 'o' && string[cur+1] == 'u' && string[cur+2] == 't' && string[cur+3] == ' ') {
                cur += 4;
                return TypeInfoVariance.OUT_VARIANCE;
            }
            else {
                return TypeInfoVariance.INVARIANT;
            }
        }

        public void parseTypes(Signature signature) {
            List<TypeInfo> types = null;
            while(cur != string.length) {
                if(types == null) {
                    types = new LinkedList<TypeInfo>();
                }
                types.add(parseType(signature));
            }
            signature.superTypes = types == null ? Collections.<TypeInfo>emptyList() : types;
        }

        private TypeInfo parseType(Signature signature) {
            switch (string[cur]) {
                case 'L':
                    cur++;
                    String name = parseName();
                    Class<?> aClass;
                    try {
                        aClass = classLoader.loadClass(name.replace('/','.'));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                    List<TypeInfoProjection> proj = null;
                    boolean nullable = false;
                    if(cur != string.length && string[cur] == '<') {
                        cur++;
                        while(string[cur] != '>') {
                            if(proj == null)
                                proj = new LinkedList<TypeInfoProjection>();
                            proj.add(parseProjection(signature));
                        }
                        cur++;
                    }
                    if(cur != string.length && string[cur] == '?') {
                        cur++;
                        nullable = true;
                    }
                    if(proj == null)
                        proj = Collections.emptyList();
                    return new TypeInfoImpl(aClass, nullable,proj.toArray(new TypeInfoProjection[proj.size()]));

                case 'T':
                    cur++;
                    return parseTypeVar(signature);

                default:
                    throw new IllegalStateException(new String(string));
            }
        }

        private TypeInfo parseTypeVar(Signature signature) {
            TypeInfoVariance variance = parseVariance();
            String klazz = parseName();
            String name = parseName();
            boolean nullable = false;
            if(string[cur] == '?') {
                nullable = true;
                cur++;
            }

            if(klazz.equals(signature.klazz.getName()))
                return new TypeInfoVar(signature, nullable, signature.varNames.get(name));
            else {
                Signature sig = signature;
                while(!klazz.equals(sig.klazz.getName())) {
                    sig = sig.outer;
                    if(sig == null)
                        throw new IllegalStateException();
                }
                
                return new TypeInfoVar(sig, nullable, sig.varNames.get(name));
            }
        }

        private TypeInfoProjection parseProjection(Signature signature) {
            final TypeInfoVariance variance = parseVariance();
            final TypeInfo type = parseType(signature);
            return new TypeInfoProjection(){
                @Override
                public TypeInfoVariance getVariance() {
                    return variance;
                }

                @Override
                public TypeInfo getType() {
                    return type;
                }

                @Override
                public String toString() {
                    return type.toString();
                }
            };
        }

        private static class ArraySignature extends Signature {
            public ArraySignature(Class klass) {
                super(null, klass);
                variables  = new LinkedList<TypeInfoProjection>();
                varNames = new HashMap<String, Integer>();
                varNames.put("T", 0);
                final TypeInfoVar typeInfoVar = new TypeInfoVar(this, 0);
                variables.add(new TypeInfoProjection(){
                    @Override
                    public TypeInfoVariance getVariance() {
                        return TypeInfoVariance.INVARIANT;
                    }

                    @Override
                    public TypeInfo getType() {
                        return typeInfoVar;
                    }
                });
            }
        }
    }
}
