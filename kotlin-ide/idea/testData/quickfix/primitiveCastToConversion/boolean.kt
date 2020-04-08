// "Replace cast with call to 'toInt()'" "false"
// WARNING: Cast can never succeed

fun foo() {
    val a = true as<caret> Int
}