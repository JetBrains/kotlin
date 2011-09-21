package jet.typeinfo;

import com.sun.org.apache.bcel.internal.generic.MethodGen;
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

    public static <T> TypeInfo<T> getTypeInfo(Class<T> klazz, boolean nullable, TypeInfoProjection[] projections) {
        return new TypeInfoImpl<T>(klazz, nullable, projections);
    }

    public abstract Object getClassObject();

    public abstract boolean isInstance(Object obj);

    public abstract int getProjectionCount();

    public abstract TypeInfoProjection getProjection(int index);

    public abstract TypeInfo getArgumentType(Class klass, int index);
    
    public abstract TypeInfo substitute(List<TypeInfo> myVars);

    private static class TypeInfoVar<T> extends TypeInfo<T> {
        final int varIndex;
        final boolean nullable;

        private TypeInfoVar(Integer varIndex) {
            this.varIndex = varIndex;
            nullable = false;
        }

        public TypeInfoVar(boolean nullable, Integer varIndex) {
            this.nullable = nullable;
            this.varIndex = varIndex;
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
        protected TypeInfo substitute(TypeInfoProjection[] projections) {
            return projections[varIndex].getType();
        }

        @Override
        public TypeInfo<?> getTypeInfo() {
            throw new UnsupportedOperationException("Abstract TypeInfo");
        }

        @Override
        public String toString() {
            return "T:" + varIndex;
        }
    }

    private static class TypeInfoImpl<T> extends TypeInfo<T> implements TypeInfoProjection {
        private TypeInfo<?> typeInfo;
        private final Signature signature;
        private final boolean nullable;
        private final TypeInfoProjection[] projections;

        TypeInfoImpl(Class<T> theClass, boolean nullable) {
            this(theClass, nullable, EMPTY);
        }

        private TypeInfoImpl(Class<T> theClass, boolean nullable, TypeInfoProjection[] projections) {
            this.signature = Parser.parse(theClass);
            this.nullable = nullable;
            this.projections = projections;
            if(signature.variables.size() != projections.length)
                throw new IllegalStateException("Wrong signature " + theClass.getName());
        }

        public final TypeInfoProjection getProjection(int index) {
            return projections[index];
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
                return getSuperTypeInfo(klass).substitute(projections).getArgumentType(klass, index);
            }
        }

        @Override
        public TypeInfo substitute(final List<TypeInfo> myVars) {
            if(projections.length == 0)
                return new TypeInfoImpl(signature.klazz, nullable, EMPTY);
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
                return new TypeInfoImpl(signature.klazz, nullable, proj);
            }
        }

        @Override
        protected TypeInfo substitute(TypeInfoProjection[] prj) {
            if(projections.length == 0)
                return new TypeInfoImpl(signature.klazz, nullable, EMPTY);
            else {
                TypeInfoProjection [] proj = new TypeInfoProjection[projections.length];
                for(int i = 0; i != proj.length; ++i) {
                    final int finalI = i;
                    final TypeInfo substitute = projections[finalI].getType().substitute(prj);
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

    protected abstract TypeInfo substitute(TypeInfoProjection[] projections);

    public static class Signature {
        final Class klazz;
        final List<TypeInfoProjection> variables;
        final List<TypeInfo> superTypes;
        
        final HashMap<Class,TypeInfo> superSignatures = new HashMap<Class,TypeInfo>();

        public Signature(Class klazz, List<TypeInfoProjection> variables, List<TypeInfo> superTypes) {
            this.klazz = klazz;
            this.superTypes = superTypes;
            this.variables = variables;
            
            List<TypeInfo> myVars = variables == null ? Collections.<TypeInfo>emptyList() : new LinkedList<TypeInfo>();
            if(variables != null)
                for(int i = 0; i != variables.size(); ++i)
                    myVars.add(new TypeInfoVar(false, i));
            
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
            List<TypeInfoProjection> vars;
            if(typeParameters == null || typeParameters.length == 0) {
                variables = Collections.emptyMap();
                vars = Collections.emptyList();
            }
            else {
                variables = new HashMap<String, Integer>();
                vars = new LinkedList<TypeInfoProjection>();
                for (int i = 0; i < typeParameters.length; i++) {
                    TypeVariable typeParameter = typeParameters[i];
                    variables.put(typeParameter.getName(), i);
                    final TypeInfoVar typeInfoVar = new TypeInfoVar(false, i);
                    vars.add(new TypeInfoProjection(){

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

            List<TypeInfo> types = new LinkedList<TypeInfo>();
            java.lang.reflect.Type genericSuperclass = klass.getGenericSuperclass();
            return new Signature(klass, vars, types);
        }

        public List<TypeInfoProjection> parseVars() {
            List<TypeInfoProjection> list = null;
            while(cur != string.length && string[cur] == 'T') {
                if(list == null) {
                    list = new LinkedList<TypeInfoProjection>();
                    variables = new HashMap<String, Integer>();
                }
                list.add(parseVar());
            }
            return list == null ? Collections.<TypeInfoProjection>emptyList() : list;
        }

        private TypeInfoProjection parseVar() {
            final TypeInfoVariance variance = parseVariance();
            String name = parseName();
            final TypeInfoVar typeInfoVar = new TypeInfoVar(variables.size());
            variables.put(name, variables.size());
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

        public List<TypeInfo> parseTypes() {
            List<TypeInfo> types = null;
            while(cur != string.length) {
                if(types == null) {
                    types = new LinkedList<TypeInfo>();
                }
                types.add(parseType());
            }
            return types == null ? Collections.<TypeInfo>emptyList() : types;
        }

        private TypeInfo parseType() {
            switch (string[cur]) {
                case 'L':
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
                            proj.add(parseProjection());
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
                    return parseTypeVar();

                default:
                    throw new IllegalStateException(new String(string));
            }
        }

        private TypeInfo parseTypeVar() {
            String name = parseName();
            boolean nullable = false;
            if(string[cur] == '?') {
                nullable = true;
                cur++;
            }

            return new TypeInfoVar(nullable, variables.get(name));
        }

        private TypeInfoProjection parseProjection() {
            final TypeInfoVariance variance = parseVariance();
            final TypeInfo type = parseType();
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
    }
}
