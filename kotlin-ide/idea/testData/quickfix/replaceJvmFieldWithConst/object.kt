// "Replace '@JvmField' with 'const'" "true"
// WITH_RUNTIME
object Foo {
    <caret>@JvmField private val a = "Lorem ipsum"
}
