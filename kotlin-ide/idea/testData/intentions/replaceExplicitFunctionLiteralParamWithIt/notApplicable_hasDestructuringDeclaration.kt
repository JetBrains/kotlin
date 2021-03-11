// WITH_RUNTIME
// IS_APPLICABLE: false

val list = listOf(Pair(1, 2), Pair(3, 4)).map { (<caret>f, s) -> f + s }
