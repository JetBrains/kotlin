// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"

@Suppress("FOO")
fun foo(): String?<caret>? = null