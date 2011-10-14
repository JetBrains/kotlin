package jet;

import jet.typeinfo.TypeInfo;

public class Tuple7<T1, T2, T3, T4, T5, T6, T7> extends DefaultJetObject{
    public final T1 _1;
    public final T2 _2;
    public final T3 _3;
    public final T4 _4;
    public final T5 _5;
    public final T6 _6;
    public final T7 _7;

    public Tuple7(TypeInfo typeInfo, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        super(typeInfo);
        _1 = t1;
        _2 = t2;
        _3 = t3;
        _4 = t4;
        _5 = t5;
        _6 = t6;
        _7 = t7;
    }

    @Override
    public String toString() {
        return "(" + _1 + ", " + _2 + ", " + _3 + ", " + _4 + ", " + _5 + ", " + _6 + ", " + _7 + ")";
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple7 t = (Tuple7) o;
        if (_1 != null ? !_1.equals(t._1) : t._1 != null) return false;
        if (_2 != null ? !_2.equals(t._2) : t._2 != null) return false;
        if (_3 != null ? !_3.equals(t._3) : t._3 != null) return false;
        if (_4 != null ? !_4.equals(t._4) : t._4 != null) return false;
        if (_5 != null ? !_5.equals(t._5) : t._5 != null) return false;
        if (_6 != null ? !_6.equals(t._6) : t._6 != null) return false;
        if (_7 != null ? !_7.equals(t._7) : t._7 != null) return false;
        return true;
    }
    @Override
    public int hashCode() {
        int result = _1 != null ? _1.hashCode() : 0;
        result = 31 * result + (_2 != null ? _2.hashCode() : 0);
        result = 31 * result + (_3 != null ? _3.hashCode() : 0);
        result = 31 * result + (_4 != null ? _4.hashCode() : 0);
        result = 31 * result + (_5 != null ? _5.hashCode() : 0);
        result = 31 * result + (_6 != null ? _6.hashCode() : 0);
        result = 31 * result + (_7 != null ? _7.hashCode() : 0);
        return result;
    }
}