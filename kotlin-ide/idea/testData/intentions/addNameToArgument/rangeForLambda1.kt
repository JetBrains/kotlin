fun foo(handler: () -> Unit){}

fun bar() {
    foo(<caret>{ })
}