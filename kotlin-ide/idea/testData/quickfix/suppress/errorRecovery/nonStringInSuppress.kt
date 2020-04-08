// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"
// ERROR: The integer literal does not conform to the expected type String

@Suppress(1)
fun foo(): String?<caret>? = null