package foo

open data class Base(val x: Int)

class Derived : Base(1) {
    fun hashCode() = 2
    fun toString() = "Derived"
}

class OverloadingHashCode1 : Base(1) {
    fun hashCode() = "not an Int"
    fun toString() = 42
}

data class OverloadingHashCode2 : Base(111) {
    fun hashCode(dummy: Int) = dummy
    fun toString(dummy: String) = dummy
}

data class DataDerived() : Base(11)

fun makeInstance(x: Int): Base {
    if (x % 3 == 0)
        return Base(x)
    else if (x % 3 == 1)
        return DataDerived()
    return Derived()
}

fun box(): String {
    val i1 = makeInstance(1)
    if (i1.hashCode() != 11) {
        return "fail: should call DataDerived.hashCode"
    }
    if (i1.toString() != "Base(x=11)") {
        return i1.toString()
    }
    val i2 = makeInstance(2)
    if (i2.hashCode() != 2) {
        return "fail: should call Derived.hashCode"
    }
    if (i2.toString() != "Derived") {
        return i2.toString()
    }
    val i3 = makeInstance(30)
    if (i3.hashCode() != 30) {
        return "fail: should call Base.hashCode"
    }
    if (i3.toString() != "Base(x=30)") {
        return i3.toString()
    }
    val i4 = DataDerived()
    if (i4.hashCode() != 11) {
        return "fail: should call DataDerived.hashCode"
    }
    if (i4.toString() != "Base(x=11)") {
        return i4.toString()
    }
    val i5 = Derived()
    if (i5.hashCode() != 2) {
        return "fail: should call Derived.hashCode"
    }
    if (i5.toString() != "Derived") {
        return i5.toString()
    }
    val i6 = Base(30)
    if (i6.hashCode() != 30) {
        return "fail: should call Base.hashCode"
    }
    if (i6.toString() != "Base(x=30)") {
        return i6.toString()
    }
    val i7 = OverloadingHashCode1()
    if (i7.hashCode() != "not an Int") {
        return "fail: should call OverloadingHashCode1.hashCode"
    }
    if (i7.toString() != 42) {
        return "${i7.toString()}"
    }
    val i8 = OverloadingHashCode2()
    if (i8.hashCode(112) != 112) {
        return "fail: should call OverloadingHashCode2.hashCode here"
    }
    if (i8.toString("ok") != "ok") {
        return "fail: should call OverloadingHashCode2.toString here"
    }
    if (i8.hashCode() != 111) {
        return "fail: should call Base.hashCode here"
    }
    if (i8.toString() != "Base(x=111)") {
        return i8.toString()
    }
    return "OK"
}
