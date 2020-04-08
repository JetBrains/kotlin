// INTENTION_TEXT: "Add 'a =' to argument"

fun foo(a: Int, b: String){}

fun bar() {
    foo(<caret>1, b = "")
}