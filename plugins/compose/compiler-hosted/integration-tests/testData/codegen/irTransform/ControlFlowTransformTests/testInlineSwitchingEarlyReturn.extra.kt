object Modifier

inline fun Modifier.thenIf(
    condition: Boolean,
    ifFalse: Modifier.() -> Modifier,
    ifTrue: Modifier.() -> Modifier,
) = if (condition) {
    ifTrue()
} else {
    ifFalse()
}

fun Modifier.clickable(f: () -> Unit): Modifier = this
