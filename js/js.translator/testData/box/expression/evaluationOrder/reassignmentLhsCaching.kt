// EXPECTED_REACHABLE_NODES: 507
package foo

var log = ""

class A(val value: Int) {
    operator fun plus(other: A): A {
        log += "A.plus(${other.value});"
        return A(value + other.value)
    }
}

val _array = arrayOf(A(2))

fun getArray(): Array<A> {
    log += "getArray();"
    return _array
}

fun getArrayIndex(): Int {
    log += "getArrayIndex();"
    return 0
}

class B(value: Int) {
    var a = A(value)
}

val _property = B(10)
val _functionResult = B(100)

val foo: B
    get() {
        log += "foo;"
        return _property
    }

fun bar(): B {
    log += "bar();"
    return _functionResult
}

fun box(): String {
    getArray()[getArrayIndex()] += A(3)
    assertEquals(5, _array[0].value)

    foo.a += A(20)
    assertEquals(30, _property.a.value)

    bar().a += A(200)
    assertEquals(300, _functionResult.a.value)

    assertEquals("getArray();getArrayIndex();A.plus(3);foo;A.plus(20);bar();A.plus(200);", log)

    return "OK"
}