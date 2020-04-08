// PROBLEM: none
// WITH_RUNTIME

val data = mutableMapOf<String, String>()
val result = data.<caret>map { "${it.key}: ${it.value}" }.joinToString("\n")