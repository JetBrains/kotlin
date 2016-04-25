package foo

@example.ExampleAnnotation
class A {
    @field:example.ExampleAnnotation
    val valA: String = "text"

    @example.ExampleAnnotation
    fun funA() {}
}