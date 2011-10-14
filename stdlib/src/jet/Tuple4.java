package jet;

import jet.typeinfo.TypeInfo;

public class Tuple4<T1, T2, T3, T4> extends DefaultJetObject {
    public final T1 _1;
    public final T2 _2;
    public final T3 _3;
    public final T4 _4;

    public Tuple4(TypeInfo typeInfo, T1 t1, T2 t2, T3 t3, T4 t4) {
        super(typeInfo);
        _1 = t1;
        _2 = t2;
        _3 = t3;
        _4 = t4;
    }

    @Override
    public String toString() {
        return "(" + _1 + ", " + _2 + ", " + _3 + ", " + _4 + ")";
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple4 t = (Tuple4) o;
        if (_1 != null ? !_1.equals(t._1) : t._1 != null) return false;
        if (_2 != null ? !_2.equals(t._2) : t._2 != null) return false;
        if (_3 != null ? !_3.equals(t._3) : t._3 != null) return false;
        if (_4 != null ? !_4.equals(t._4) : t._4 != null) return false;
        return true;
    }
    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + (_2 != null ? _2.hashCode() : 0);
        result = 31 * result + (_3 != null ? _3.hashCode() : 0);
        result = 31 * result + (_4 != null ? _4.hashCode() : 0);
        return result;
    }
}