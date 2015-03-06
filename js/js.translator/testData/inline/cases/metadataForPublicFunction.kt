package foo

// CHECK_CONTAINS_NO_CALLS: test1
// CHECK_CONTAINS_NO_CALLS: test2
// CHECK_CONTAINS_NO_CALLS: test3
// CHECK_CONTAINS_NO_CALLS: test4
// CHECK_CONTAINS_NO_CALLS: test5
// CHECK_HAS_INLINE_METADATA: apply_hiyix$
// CHECK_HAS_INLINE_METADATA: applyL_hiyix$
// CHECK_HAS_INLINE_METADATA: applyM_hiyix$
// CHECK_HAS_NO_INLINE_METADATA: applyN
// CHECK_HAS_NO_INLINE_METADATA: applyO_hiyix$

inline
public fun apply<T>(arg: T, func: (T)->T): T = func(arg)

public open class L {
    inline
    protected fun applyL<T>(arg: T, func: (T)->T): T = func(arg)
}

public class M {
    inline
    public fun applyM<T>(arg: T, func: (T)->T): T = func(arg)
}

private class N {
    inline
    public fun applyN<T>(arg: T, func: (T)->T): T = func(arg)
}

private object O {
    public object OInner {
        inline
        public fun applyO<T>(arg: T, func: (T)->T): T = func(arg)
    }
}

fun test1(x: Int, y: Int): Int = apply(x) { it * y }

fun test2(m: M, x: Int, y: Int): Int = m.applyM(x) { it * y }

fun test3(n: N, x: Int, y: Int): Int = n.applyN(x) { it * y }

object LTest : L() {
    fun test4(l: L, x: Int, y: Int): Int = l.applyL(x) { it * y }
}

fun test5(x: Int, y: Int): Int = O.OInner.applyO(x) { it * y }

fun box(): String {
    assertEquals(6, test1(2, 3))
    assertEquals(20, test1(5, 4))

    assertEquals(6, test2(M(), 2, 3))
    assertEquals(20, test2(M(), 5, 4))

    assertEquals(6, test3(N(), 2, 3))
    assertEquals(20, test3(N(), 5, 4))

    assertEquals(6, LTest.test4(L(), 2, 3))
    assertEquals(20, LTest.test4(L(), 5, 4))

    assertEquals(6, test5(2, 3))
    assertEquals(20, test5(5, 4))

    return "OK"
}