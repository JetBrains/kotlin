package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public abstract class ByteIterator implements Iterator<Byte> {
    private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(ByteIterator.class, false);

    public TypeInfo getTypeInfo () {
        return typeInfo;
    }

    public final Byte next() {
        return nextByte();
    }

    public abstract byte nextByte();
}
