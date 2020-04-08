// PROBLEM: none
// WITH_RUNTIME

val x = listOf('1', 'a', 0.toChar()).<caret>filter { it.toInt() != 0 }.first(Char::isLetter)