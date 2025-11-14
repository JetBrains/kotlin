// RUN_PIPELINE_TILL: FRONTEND

fun whenWithoutSubjectBooleanConditions(x: Int, y: Int): String =
    when {
        x == y -> "eq"
        x < y -> "lt"
        else -> "gt"
    }