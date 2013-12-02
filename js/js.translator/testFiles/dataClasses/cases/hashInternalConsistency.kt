package foo

data class TwoThings(val x: Long, val y: Boolean)

data class ManyThings(val x: Int,
                      val y: Short,
                      val z: String,
                      val t: Char,
                      val v: Array<TwoThings?>)

fun box(): String {
    var t1 = ManyThings(3, 42, "pi", 'a', array(TwoThings(0xdeadbeef, true),
                                                null,
                                                TwoThings(0xcafebabe, true)))
    var t2 = t1.copy();
    var t3 = t1.copy(v = array(TwoThings(0xdeadbeef, true),
                               TwoThings(0xcafebabe, true),
                               null))
    var t4 = t1.copy(x = 4)
    var t5 = t1.copy(y = 98)
    var t6 = t1.copy(z = "py")
    var t7 = t1.copy(v = array(TwoThings(0xdeadbeef, true),
                               null,
                               TwoThings(0xcafebabe, false)))
    if (t1.hashCode() != t2.hashCode()) {
        return "fail: t1 hash != t2 hash"
    }
    if (t1.hashCode() == t3.hashCode()) {
        return "fail: t1 hash == t3 hash"
    }
    if (t1.hashCode() == t4.hashCode()) {
        return "fail: t1 hash == t4 hash"
    }
    if (t1.hashCode() == t5.hashCode()) {
        return "fail: t1 hash == t5 hash"
    }
    if (t1.hashCode() == t6.hashCode()) {
        return "fail: t1 hash == t6 hash"
    }
    if (t1.hashCode() == t7.hashCode()) {
        return "fail: t1 hash == t7 hash"
    }
    return "OK"
}
