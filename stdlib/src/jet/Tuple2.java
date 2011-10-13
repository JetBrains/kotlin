package jet;

import jet.typeinfo.TypeInfo;

public class Tuple2<T1, T2> extends DefaultJetObject {
    public final T1 _1;
    public final T2 _2;

    public Tuple2(TypeInfo typeInfo, T1 t1, T2 t2) {
        super(typeInfo);
        _1 = t1;
        _2 = t2;
    }

    @Override
    public String toString() {
        return "(" + _1 + ", " + _2 + ")";
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple2 t = (Tuple2) o;
        if (_1 != null ? !_1.equals(t._1) : t._1 != null) return false;
        if (_2 != null ? !_2.equals(t._2) : t._2 != null) return false;
        return true;
    }
    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + (_2 != null ? _2.hashCode() : 0);
        return result;
    }
}