package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public abstract class BooleanIterator implements Iterator<Boolean> {
    private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(BooleanIterator.class, false);

    public TypeInfo getTypeInfo () {
        return typeInfo;
    }

    public final Boolean next() {
        return nextBoolean();
    }

    public abstract boolean nextBoolean();
}
