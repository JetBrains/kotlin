object Foo {
    const val NULL = "null_as_string"
    var DEBUG = false

    @ObjCName("NO")
    val Bar = "no_as_string"

    // Acceptable as a property name. Neither it, nor its selector should be mangled.
    val load = "load_as_string"

    // Needs a mangled getter as the property's name violates the naming rules.
    // Its setter, however, should not be exposed as it'd be in the form of `set<propertyName>:`.
    var release = "release_as_string"
}

val YES = Foo.NULL

fun f() {
    Foo.release = "release"
}
