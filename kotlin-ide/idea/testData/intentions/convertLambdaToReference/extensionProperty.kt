val Any.name: String get() = toString()

val converted = { x: Any -> <caret>x.name }