// CHOOSE_USE_SITE_TARGET: get
// IS_APPLICABLE: false

annotation class A

class Test {
    @get:A
    @A<caret>
    val foo: String = ""
}