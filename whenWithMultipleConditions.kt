// RUN_PIPELINE_TILL: FRONTEND

fun whenWithMultipleConditions(x: Int): String =
    when (x) {
        0, 1, 2 -> "small"
        3, 4 -> "medium"
        else -> "other"
    }