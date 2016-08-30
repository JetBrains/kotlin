package foo

class A() {

}

fun box(): String {
    assertEquals(true, A() is A)
    return "OK"
}