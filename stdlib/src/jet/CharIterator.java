package jet;

import jet.typeinfo.TypeInfo;

/**
 * @author alex.tkachman
 */
public abstract class CharIterator implements Iterator<Character> {
    private static final TypeInfo typeInfo = TypeInfo.getTypeInfo(CharIterator.class, false);

    public TypeInfo getTypeInfo () {
        return typeInfo;
    }

    public final Character next() {
        return nextChar();
    }

    public abstract char nextChar();
}
