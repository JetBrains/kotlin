package bar

import foo.A

@example.ExampleAnnotation
class B {
    @field:example.ExampleAnnotation
    val valB = "text"

    @example.ExampleAnnotation
    fun funB() {}

    fun useAfromB(a: A) {}
}