package jet.typeinfo;

import org.jetbrains.annotations.NotNull;

/**
 * @author alex.tkachman
 */
public interface TypeInfoProjection {
    @NotNull
    TypeInfoVariance getVariance();

    @NotNull
    TypeInfo getType();

    abstract class TypeInfoProjectionImpl implements TypeInfoProjection {
        @NotNull private final TypeInfo type;

        TypeInfoProjectionImpl(@NotNull TypeInfo typeInfo) {
            this.type = typeInfo;
        }

        @Override
        @NotNull
        public final TypeInfo getType() {
            return type;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TypeInfoProjectionImpl that = (TypeInfoProjectionImpl) o;
            // no need to compare variance as we compared classes already
            return type.equals(that.type);
        }

        @Override
        public final int hashCode() {
            int result = type.hashCode();
            result = 31 * result + (getVariance().hashCode());
            return result;
        }

        @Override
        public final String toString() {
            return (getVariance() == TypeInfoVariance.INVARIANT ? "" : getVariance().toString() + " ") + type;
        }
    }
}
