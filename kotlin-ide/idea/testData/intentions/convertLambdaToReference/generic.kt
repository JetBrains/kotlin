// IS_APPLICABLE: false

fun <T> id(y: T) = y

val x = { arg: Int <caret>-> id<Int>(arg) }