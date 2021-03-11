// PROBLEM: none
// WITH_RUNTIME

fun String.toNullableInt(): Int? = null

val x = listOf("1").<caret>mapNotNull(String::toNullableInt)