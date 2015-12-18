package kotlin.test

internal fun <T, R> T.let(block: (T) -> R): R = block(this)

