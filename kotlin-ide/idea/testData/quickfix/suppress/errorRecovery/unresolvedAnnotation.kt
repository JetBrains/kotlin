// "Suppress 'REDUNDANT_NULLABLE' for fun foo" "true"
// ERROR: Unresolved reference: ann

@ann fun foo(): String?<caret>? = null