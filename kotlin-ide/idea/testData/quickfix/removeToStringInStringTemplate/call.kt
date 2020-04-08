// "Remove 'toString()' call" "true"

operator fun Any.invoke() = this

fun foo(arg: Any) = "${arg().<caret>toString()}"