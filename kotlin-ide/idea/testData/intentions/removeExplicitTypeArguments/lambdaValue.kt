// IS_APPLICABLE: true
// WITH_RUNTIME

fun foo(): List<String> = run {
    listOf<caret><String>()
}