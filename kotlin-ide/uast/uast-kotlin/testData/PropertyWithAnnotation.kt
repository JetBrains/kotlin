annotation class TestAnnotation

@TestAnnotation
val prop1: Int = 0

@get:TestAnnotation
val prop2: Int
    get() = 0

@set:TestAnnotation
var prop3: Int = 0
    get() = 0
    set(value) { field = value }