// DISABLE-ERRORS
class A(var n: Int) {
    fun plusAssign(m: Int) {
        n += m
    }
}

class Foo {
    init {
        var a = Array(2) { A(it) }
        <selection>a[1] += 2</selection>
        a[1] = a[1] + 2
        a.set(1, a[1] + 2)
        a.set(1, a.get(1).plus(2))
        a[1] + 2
        a.get(1).plus(2)
        a[1].plusAssign(2)
        a.get(1).plusAssign(2)
    }
}