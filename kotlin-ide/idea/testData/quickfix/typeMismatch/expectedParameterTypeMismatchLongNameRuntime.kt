// "Change type from 'String' to 'HashSet<Int>'" "true"

fun foo(f: (java.util.HashSet<Int>) -> String) {
    foo {
        x: String<caret> -> ""
    }
}