// CHOOSE_USE_SITE_TARGET: property

annotation class A

class Property {
    @A<caret>
    val foo: String
        get() = ""
}