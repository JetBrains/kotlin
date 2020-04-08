// IS_APPLICABLE: false

annotation class A

class Test {
    @get:A<caret>
    val foo: String = ""
}