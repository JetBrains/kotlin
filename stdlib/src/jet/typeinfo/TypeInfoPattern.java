package jet.typeinfo;

/**
 * This class represents how type info of super class should be matched against type parameters of subclass.
 *
 * For each subclass we create such pattern for subclass itself and all it super classes either during
 * static initialization(or maybe when needed).
 *
 * For each instance of parametrized type we keep it type info, which can be understood as binding of type parameter variables.
 *
 * @author alex.tkachman
 */
public interface TypeInfoPattern {
    TypeInfo substitute(TypeInfo[] variables);

    class Var implements TypeInfoPattern {
        public final int index;

        public Var(int index) {
            this.index = index;
        }

        @Override
        public TypeInfo substitute(TypeInfo[] variables) {
            return variables[index];
        }
    }

    class Const implements TypeInfoPattern {
        private final TypeInfo typeInfo;

        public Const(TypeInfo typeInfo) {
            this.typeInfo = typeInfo;
        }

        @Override
        public TypeInfo substitute(TypeInfo[] variables) {
            return typeInfo;
        }
    }

    class Pattern implements TypeInfoPattern {
        public static final TypeInfoPatternProjection[] EMPTY = new TypeInfoPatternProjection[0];
        public final Class klazz;
        public final boolean nullable;
        public final TypeInfoPatternProjection [] patterns;

        public Pattern(Class klazz, boolean nullable, TypeInfoPatternProjection[] patterns) {
            this.klazz = klazz;
            this.nullable = nullable;
            this.patterns = patterns;
        }

        public Pattern(Class klazz, boolean nullable) {
            this.klazz = klazz;
            this.nullable = nullable;
            this.patterns = EMPTY;
        }

        @Override
        public TypeInfo substitute(TypeInfo[] variables) {
            return TypeInfo.getTypeInfo(klazz, nullable, TypeInfoPatternProjection.substitute(patterns, variables));
        }
    }
    
    class TypeInfoPatternProjection {
        public final TypeInfoVariance variance;

        public final TypeInfoPattern pattern;

        public TypeInfoPatternProjection(TypeInfoVariance variance, TypeInfoPattern pattern) {
            this.variance = variance;
            this.pattern = pattern;
        }

        public TypeInfoProjection substitute(final TypeInfo[] variables) {
            return new TypeInfoProjection() {
                @Override
                public TypeInfoVariance getVariance() {
                    return variance;
                }

                @Override
                public TypeInfo getType() {
                    return pattern.substitute(variables);
                }
            };
        }

        public static TypeInfoProjection[] substitute(TypeInfoPatternProjection[] patterns, TypeInfo[] variables) {
            if(patterns.length == 0)
                return TypeInfoProjection.EMPTY_ARRAY;
            TypeInfoProjection [] projections = new TypeInfoProjection[patterns.length];
            for (int i = 0; i < projections.length; i++) {
                projections[i] = patterns[i].substitute(variables);
            }
            return projections;
        }
    }
}
