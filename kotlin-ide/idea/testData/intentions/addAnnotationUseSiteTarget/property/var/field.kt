// CHOOSE_USE_SITE_TARGET: field

annotation class A

class Property {
    @A<caret>
    var foo: String = ""
}