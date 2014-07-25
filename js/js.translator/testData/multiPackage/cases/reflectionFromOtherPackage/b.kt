package foo

import bar.*

fun A.ext2(s: String): String = "A.ext2: ${this.v} ${s}"

fun box(): Boolean {

    assertEquals("topLevelFun: A", (::topLevelFun)("A"))
    assertEquals("A.ext1: test B", A("test").(A::ext1)("B"))
    assertEquals("A.ext2: test B", A("test").(A::ext2)("B"))
    assertEquals("memA: test C", A("test").(A::memA)("C"))

    assertEquals(100, ::topLevelVar.get())
    ::topLevelVar.set(500)
    assertEquals(500, ::topLevelVar.get())
    assertEquals(200, ::topLevelVal.get())
    val a = A("test")
    assertEquals(1000, (A::propVar).get(a))
    A::propVar.set(a, 5000)
    assertEquals(5000, (A::propVar).get(a))
    assertEquals(2000, (A::propVal).get(a))

    assertEquals("text", (A::extProp).get(a))
    (A::extProp).set(a, "new text")
    assertEquals("new text", (A::extProp).get(a))

    return true;
}