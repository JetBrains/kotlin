class Foo<Bar> {
    val Bar.extensionProperty: Int get() = 42
    fun Bar.extensionFunction() = Unit
}

val Foo<Any>.topLevelExtensionProperty: Int get() = 42