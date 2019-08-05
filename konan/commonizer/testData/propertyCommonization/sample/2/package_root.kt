// root package

val foo = 1

val bar = 42
val String.bar get() = this
val Int.bar get() = this

var baz = "baz"
var bazWithSetter = "baz"
    set
var bazWithCustomSetter = "baz"
    set(value) {
        field = value.toLowerCase()
    }
var bazWithSetterWithInternalVisibility = "baz"
    internal set
var bazWithCustomSetterWithInternalVisibility = "baz"
    internal set(value) {
        field = value.toLowerCase()
    }
