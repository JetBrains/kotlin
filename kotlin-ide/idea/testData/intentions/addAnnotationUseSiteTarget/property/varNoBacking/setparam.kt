// CHOOSE_USE_SITE_TARGET: setparam

annotation class A

class Property {
    @A<caret>
    var foo: String
        get() = ""
        set(p) {}
}