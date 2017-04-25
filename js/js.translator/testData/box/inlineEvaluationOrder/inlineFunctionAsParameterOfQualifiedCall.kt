// EXPECTED_REACHABLE_NODES: 1446
// See KT-11711
package foo

interface A {
    val b: B
}

interface B {
    fun c(a: Any?)
}

val a: A
    get() {
        log("a.get")
        return object : A {
            override val b: B
                get() {
                    log("b.get")
                    return object : B {
                        override fun c(a: Any?) {
                            log("c()")
                        }
                    }
                }
        }
    }

val g: Any?
    get() {
        log("g.get")
        return "c"
    }

inline fun foo(): Any? {
    log("foo()")
    return g;
}

inline fun bar(): Any? {
    return g;
}

inline fun baz(): Any? {
    return log("baz()");
}

inline fun boo(a: Any?): Any? {
    return log("boo()");
}

fun box(): String {
    log("--1--")
    a.b.c(g)

    log("--2--")

    a.b.c(foo())

    log("--3--")

    a.b.c(bar())

    log("--4--")

    a.b.c(baz())

    log("--5--")

    a.b.c(boo(g))

    assertEquals("""--1--
a.get
b.get
g.get
c()
--2--
a.get
b.get
foo()
g.get
c()
--3--
a.get
b.get
g.get
c()
--4--
a.get
b.get
baz()
c()
--5--
a.get
b.get
g.get
boo()
c()
""", pullLog().replace(';', '\n'))

    return "OK"
}