package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public abstract class ShortIterator implements Iterator<Short> {
    private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ShortIterator.class, false);

    public TypeInfo getTypeInfo () {
        return typeInfo;
    }

    public final Short next() {
        return nextShort();
    }

    public abstract short nextShort();
}
