enum class Enum(x: String) {
    A("a"),
    B("b");

    val becameNullable: Any = x
    val unchanged: Any = x
}

fun Any.string() = this as String