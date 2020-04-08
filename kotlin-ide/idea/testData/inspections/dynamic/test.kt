class A {
    fun foo(i: Int) {}
}

fun bar(d: dynamic, s: String, a: Any, u: Unit) {}

fun main(args: Array<String>) {
    val d: dynamic = Any()
    var s: String = ""
    var a: Any = ""
    var u: Unit = Unit

    s = d
    a = d
    u = d

    s = d as String
    a = d as Any
    u = d as Unit

    if (d is String) {
        s = d
        d.subSequence(1, 2)
    }

    if (d is A) {
        d.foo(1)
    }

    if (a is String) {
        s = a
        a.length
    }

    if (d is Any) {
        a = d
    }

    if (d is Unit) {
        u = d
    }

    bar(d, d.boo, d, d)
    bar(d, d as String, d as Any, d as Unit)
    bar(d.aaa, d.bbb as String, d.ccc(), d.ddd {})

    return d
}
