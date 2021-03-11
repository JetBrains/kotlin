// IS_APPLICABLE: false

fun foo(vararg s: String){}

fun bar() {
    foo(""<caret>)
}