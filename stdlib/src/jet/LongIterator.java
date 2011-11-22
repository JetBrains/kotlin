package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public abstract class LongIterator implements Iterator<Long> {
    private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(LongIterator.class, false);

    public TypeInfo getTypeInfo () {
        return typeInfo;
    }

    public final Long next() {
        return nextLong();
    }

    public abstract long nextLong();
}
