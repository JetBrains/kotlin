package stepOverInlinedLambda

fun main(args: Array<String>) {
    //Breakpoint!
    val a = A()
    foo { test(1) }
    foo {
        test(2)
    }
    a.foo { test(3) }.foo { test(4) }
    a.foo {
        test(5)
    }.foo {
        test(6)
    }
    a.foo { test(7) }
       .foo { test(8) }

    foo({ test(9) }) { test(10) }
    foo({ test(11) }) {
        test(12)
    }
    foo({
            test(13)
        }, {
        test(14)
    })

    val b = foo { test(1) }
}

inline fun foo(f: () -> Unit) {
    f()
}

inline fun foo(f1: () -> Unit, f2: () -> Unit) {
    f1()
    f2()
}

class A {
    inline fun foo(f: () -> Unit): A {
        f()
        return this
    }
}

fun test(i: Int) = 1

// STEP_OVER: 12