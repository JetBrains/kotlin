package foo

class Foo {
    fun blah(value: Int): Int {
        return value + 1
    }
}

val Foo.fooImp: Int
    get() {
        return blah(5)
    }

val Foo.fooExp: Int
    get() {
        return this.blah(10)
    }

fun box(): Boolean {
    var a = Foo()
    if (a.fooImp != 6) return false
    if (a.fooExp != 11) return false
    return true;
}
