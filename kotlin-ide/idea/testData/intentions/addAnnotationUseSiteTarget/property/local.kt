// IS_APPLICABLE: false

annotation class A

class Property {
    fun test() {
        @A<caret>
        val foo: String = ""
    }
}