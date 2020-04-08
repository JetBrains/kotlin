// PROBLEM: none
// WITH_RUNTIME

fun plusNullable(arg: String?) = arg?.let<caret> { it + "#" } ?: ""