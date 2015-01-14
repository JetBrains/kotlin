package foo

class A

fun box(): String {
    assertEquals("number", typeof(1))
    assertEquals("number", typeof(1.2))
    assertEquals("boolean", typeof(true))
    assertEquals("string", typeof("sss"))
    assertEquals("object", typeof(null))
    assertEquals("undefined", typeof(undefined))
    assertEquals("object", typeof(object {}))
    assertEquals("object", typeof(A()))

    return "OK"
}