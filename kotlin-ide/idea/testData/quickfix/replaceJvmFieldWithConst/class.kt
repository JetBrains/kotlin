// "Replace '@JvmField' with 'const'" "false"
// WITH_RUNTIME
// ERROR: JvmField has no effect on a private property
// ACTION: Convert to lazy property
// ACTION: Make internal
// ACTION: Make protected
// ACTION: Make public
// ACTION: Move to constructor
// ACTION: Specify type explicitly
// ACTION: Add use-site target 'field'
class Foo {
    <caret>@JvmField private val a = "Lorem ipsum"
}
