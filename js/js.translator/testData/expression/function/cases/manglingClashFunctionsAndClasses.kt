package foo

public class A

internal fun A(a: Int){}

public class B(a: Int)

internal fun B(){}

internal fun C(a: Int){}

public class C

internal fun D(){}

public class D(a: Int)

//Testing

internal fun testClass(name: String, f: () -> Unit) {
    val fs = f.toString()

    if ("$name(" !in fs) throw Exception("Name of class '$name' unexpectedly mangled: $fs")
}

internal fun testFun(name: String, f: () -> Unit) {
    val fs = f.toString()

    if ("$name(" in fs) throw Exception("Name of fun '$name' unexpectedly not mangled: $fs")
}


fun box(): String {
    testClass("A") { A() }
    testFun("A") { A(1) }
    testFun("B") { B() }
    testClass("B") { B(1) }
    testClass("C") { C() }
    testFun("C") { C(1) }
    testFun("D") { D() }
    testClass("D") { D(1) }

    return "OK"
}
