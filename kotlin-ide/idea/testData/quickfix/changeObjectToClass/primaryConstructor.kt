// "Change 'object' to 'class'" "true"
annotation class Ann

// comment
@Ann
object Foo<caret>(val s: String) : Any() {
    constructor() : this("")
}