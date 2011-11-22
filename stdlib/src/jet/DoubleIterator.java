package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public abstract class DoubleIterator implements Iterator<Double> {
    private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(DoubleIterator.class, false);

    public TypeInfo getTypeInfo () {
        return typeInfo;
    }

    public final Double next() {
        return nextDouble();
    }

    public abstract double nextDouble();
}
