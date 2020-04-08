// CHOOSE_USE_SITE_TARGET: delegate
// IS_APPLICABLE: false

annotation class A

class Property {
    @A<caret>
    var foo: String = ""
}