// WITH_RUNTIME

val x = "5abc".<caret>filter { it.isDigit() }.singleOrNull()