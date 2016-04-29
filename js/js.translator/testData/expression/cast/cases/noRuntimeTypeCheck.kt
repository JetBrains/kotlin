package foo

// CHECK_NOT_CALLED_IN_SCOPE: scope=box function=isType

open class A()

class B() : A()

fun box(): String {
    assertTrue(B() is A)
    assertTrue(B() is A?)
    assertTrue(B() is B)
    assertTrue(B() is B?)
    assertTrue((B() as B?) is A?)
    assertTrue((null as A?) is A?)

    assertNotEquals(null, B() as? A)
    assertNotEquals(null, B() as? A?)
    assertNotEquals(null, B() as? B)
    assertNotEquals(null, B() as? B?)

    assertNotEquals(null, B() as A)
    assertNotEquals(null, B() as A?)
    assertNotEquals(null, B() as B)
    assertNotEquals(null, B() as B?)

    return "OK"
}
