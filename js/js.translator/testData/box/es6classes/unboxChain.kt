// EXPECTED_REACHABLE_NODES: 1371

inline class I1(val a: Int)
inline class I2(val i: I1)
inline class I3(val i: I2)
inline class I4(val i: I3)
inline class I5(val i: I4)

class TestDefault(val def: I5 = I5(I4(I3(I2(I1(999))))))

class TestGen<T>(val gen: T)

fun box(): String {
    val x = I5(I4(I3(I2(I1(1337)))))
    assertEquals(1337, x.i.i.i.i.a)

    val testDefault = TestDefault()
    assertEquals(999, testDefault.def.i.i.i.i.a)

    val testDefaultGen = TestGen(I5(I4(I3(I2(I1(1953))))))
    assertTrue(testDefault.def.i.i.i.i.a is Int)
    assertEquals(1953, testDefaultGen.gen.i.i.i.i.a)

    return "OK"
}
