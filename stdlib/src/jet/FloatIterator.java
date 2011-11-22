package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public abstract class FloatIterator implements Iterator<Float> {
    private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(FloatIterator.class, false);

    public TypeInfo getTypeInfo () {
        return typeInfo;
    }

    public final Float next() {
        return nextFloat();
    }

    public abstract float nextFloat();
}
