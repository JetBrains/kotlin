package jet;

import jet.typeinfo.TypeInfo;

public class Tuple20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> extends DefaultJetObject{
    public final T1 _1;
    public final T2 _2;
    public final T3 _3;
    public final T4 _4;
    public final T5 _5;
    public final T6 _6;
    public final T7 _7;
    public final T8 _8;
    public final T9 _9;
    public final T10 _10;
    public final T11 _11;
    public final T12 _12;
    public final T13 _13;
    public final T14 _14;
    public final T15 _15;
    public final T16 _16;
    public final T17 _17;
    public final T18 _18;
    public final T19 _19;
    public final T20 _20;

    public Tuple20(TypeInfo typeInfo, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11, T12 t12, T13 t13, T14 t14, T15 t15, T16 t16, T17 t17, T18 t18, T19 t19, T20 t20) {
        super(typeInfo);
        _1 = t1;
        _2 = t2;
        _3 = t3;
        _4 = t4;
        _5 = t5;
        _6 = t6;
        _7 = t7;
        _8 = t8;
        _9 = t9;
        _10 = t10;
        _11 = t11;
        _12 = t12;
        _13 = t13;
        _14 = t14;
        _15 = t15;
        _16 = t16;
        _17 = t17;
        _18 = t18;
        _19 = t19;
        _20 = t20;
    }

    @Override
    public String toString() {
        return "(" + _1 + ", " + _2 + ", " + _3 + ", " + _4 + ", " + _5 + ", " + _6 + ", " + _7 + ", " + _8 + ", " + _9 + ", " + _10 + ", " + _11 + ", " + _12 + ", " + _13 + ", " + _14 + ", " + _15 + ", " + _16 + ", " + _17 + ", " + _18 + ", " + _19 + ", " + _20 + ")";
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tuple20 t = (Tuple20) o;
        if (_1 != null ? !_1.equals(t._1) : t._1 != null) return false;
        if (_2 != null ? !_2.equals(t._2) : t._2 != null) return false;
        if (_3 != null ? !_3.equals(t._3) : t._3 != null) return false;
        if (_4 != null ? !_4.equals(t._4) : t._4 != null) return false;
        if (_5 != null ? !_5.equals(t._5) : t._5 != null) return false;
        if (_6 != null ? !_6.equals(t._6) : t._6 != null) return false;
        if (_7 != null ? !_7.equals(t._7) : t._7 != null) return false;
        if (_8 != null ? !_8.equals(t._8) : t._8 != null) return false;
        if (_9 != null ? !_9.equals(t._9) : t._9 != null) return false;
        if (_10 != null ? !_10.equals(t._10) : t._10 != null) return false;
        if (_11 != null ? !_11.equals(t._11) : t._11 != null) return false;
        if (_12 != null ? !_12.equals(t._12) : t._12 != null) return false;
        if (_13 != null ? !_13.equals(t._13) : t._13 != null) return false;
        if (_14 != null ? !_14.equals(t._14) : t._14 != null) return false;
        if (_15 != null ? !_15.equals(t._15) : t._15 != null) return false;
        if (_16 != null ? !_16.equals(t._16) : t._16 != null) return false;
        if (_17 != null ? !_17.equals(t._17) : t._17 != null) return false;
        if (_18 != null ? !_18.equals(t._18) : t._18 != null) return false;
        if (_19 != null ? !_19.equals(t._19) : t._19 != null) return false;
        if (_20 != null ? !_20.equals(t._20) : t._20 != null) return false;
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
        result = 31 * result + (_8 != null ? _8.hashCode() : 0);
        result = 31 * result + (_9 != null ? _9.hashCode() : 0);
        result = 31 * result + (_10 != null ? _10.hashCode() : 0);
        result = 31 * result + (_11 != null ? _11.hashCode() : 0);
        result = 31 * result + (_12 != null ? _12.hashCode() : 0);
        result = 31 * result + (_13 != null ? _13.hashCode() : 0);
        result = 31 * result + (_14 != null ? _14.hashCode() : 0);
        result = 31 * result + (_15 != null ? _15.hashCode() : 0);
        result = 31 * result + (_16 != null ? _16.hashCode() : 0);
        result = 31 * result + (_17 != null ? _17.hashCode() : 0);
        result = 31 * result + (_18 != null ? _18.hashCode() : 0);
        result = 31 * result + (_19 != null ? _19.hashCode() : 0);
        result = 31 * result + (_20 != null ? _20.hashCode() : 0);
        return result;
    }
}