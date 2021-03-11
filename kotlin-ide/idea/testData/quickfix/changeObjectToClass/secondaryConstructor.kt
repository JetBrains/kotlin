// "Change 'object' to 'class'" "true"
annotation class Ann

// comment
@Ann
object Foo(val s: String) : Any() {
    <caret>constructor() : this("")
}