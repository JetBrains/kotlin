package foo

// CHECK_CONTAINS_NO_CALLS: test1_0
// CHECK_CONTAINS_NO_CALLS: test2_0
// CHECK_CONTAINS_NO_CALLS: test3_0
// CHECK_CONTAINS_NO_CALLS: test4_buocd8$
// CHECK_CONTAINS_NO_CALLS: test5_0
// CHECK_HAS_INLINE_METADATA: apply_hiyix$
// CHECK_HAS_INLINE_METADATA: applyL_0
// CHECK_HAS_INLINE_METADATA: applyM_hiyix$
// CHECK_HAS_NO_INLINE_METADATA: applyN_0
// CHECK_HAS_NO_INLINE_METADATA: applyO_hiyix$

inline
public fun <T> apply(arg: T, func: (T)->T): T = func(arg)

public open class L {
    inline
    protected fun <T> applyL(arg: T, func: (T)->T): T = func(arg)
    fun test4(l: L, x: Int, y: Int): Int = l.applyL(x) { it * y }
}

public class M {
    inline
    public fun <T> applyM(arg: T, func: (T)->T): T = func(arg)
}

internal class N {
    inline
    public fun <T> applyN(arg: T, func: (T)->T): T = func(arg)
}

private object O {
    public object OInner {
        inline
        public fun <T> applyO(arg: T, func: (T)->T): T = func(arg)
    }
}

internal fun test1(x: Int, y: Int): Int = apply(x) { it * y }

internal fun test2(m: M, x: Int, y: Int): Int = m.applyM(x) { it * y }

internal fun test3(n: N, x: Int, y: Int): Int = n.applyN(x) { it * y }

internal fun test5(x: Int, y: Int): Int = O.OInner.applyO(x) { it * y }

fun box(): String {
    assertEquals(6, test1(2, 3))
    assertEquals(20, test1(5, 4))

    assertEquals(6, test2(M(), 2, 3))
    assertEquals(20, test2(M(), 5, 4))

    assertEquals(6, test3(N(), 2, 3))
    assertEquals(20, test3(N(), 5, 4))

    assertEquals(6, L().test4(L(), 2, 3))
    assertEquals(20, L().test4(L(), 5, 4))

    assertEquals(6, test5(2, 3))
    assertEquals(20, test5(5, 4))

    return "OK"
}