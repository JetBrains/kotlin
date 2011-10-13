package jet;

import jet.typeinfo.TypeInfo;

public class Tuple1<T1> extends DefaultJetObject {
    public final T1 _1;

    public Tuple1(TypeInfo typeInfo, T1 t1) {
        super(typeInfo);
        _1 = t1;
    }

    @Override
    public String toString() {
        return "(" + _1 + ")";
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple1 t = (Tuple1) o;
        if (_1 != null ? !_1.equals(t._1) : t._1 != null) return false;
        return true;
    }
    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        return result;
    }
}