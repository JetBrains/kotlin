package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public class Tuple0 implements JetObject {
    public static final Tuple0 INSTANCE = new Tuple0();
    private static final TypeInfo<?> typeInfo = TypeInfo.getTypeInfo(Tuple0.class, false);

    private Tuple0() {
    }

    @Override
    public String toString() {
        return "()";
    }

    @Override
    public boolean equals(Object o) {
        return o == INSTANCE;
    }

    @Override
    public int hashCode() {
        return 239;
    }

    @Override
    public TypeInfo<?> getTypeInfo() {
        return typeInfo;
    }
}