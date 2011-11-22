package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public abstract class IntIterator implements Iterator<Integer> {
    private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(IntIterator.class, false);

    public TypeInfo getTypeInfo () {
        return typeInfo;
    }

    public final Integer next() {
        return nextInt();
    }

    public abstract int nextInt();
}
