// "Remove 'toString()' call" "true"

fun foo(arg: Any) = "arg = ${arg.<caret>toString()}"