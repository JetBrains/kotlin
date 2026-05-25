inline fun thenIf(
    condition: Boolean,
    ifFalse: () -> Unit,
    ifTrue: () -> Unit,
) = if (condition) {
    ifTrue()
} else {
    ifFalse()
}
