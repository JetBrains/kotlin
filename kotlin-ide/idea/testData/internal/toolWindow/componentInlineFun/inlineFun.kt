package inlineFun1

class A {
    inline operator fun component1() = foo { 1 }
    inline operator fun component2() = foo { 2 }

    inline fun foo(f: () -> Int) = f()
}