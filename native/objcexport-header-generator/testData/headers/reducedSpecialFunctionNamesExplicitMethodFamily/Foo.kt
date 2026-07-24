class Foo {
    // When explicitMethodFamily is set to true, an accompanying method is generated for properties whose name is "special",
    // i.e. starts with one of these words: "alloc", "copy", "mutableCopy", "new", "init".
    val copySomething = 1
    val mutableCopy = 2
    val newBar = 3
    fun alloc(): Int = 4

    // Irrespective of explicitMethodFamily's value, a property's getter and setter are prefixed with "do" should its name be
    // be "init" or start with "initWith".
    val initWithSomething = 5
    var initWithNothing = 6

    // These names are special too as leading underscores are stripped when checking if a name is special.
    val _newBaz: Int = 7
    fun __initSomethingElse(): Int = 8
    val copyWithSomething = 9
}
