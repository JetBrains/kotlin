// PROBLEM: none
// WITH_RUNTIME

val x = mapOf(1 to 2, 3 to 4).<caret>map { (key, value) -> key + value }.joinToString()