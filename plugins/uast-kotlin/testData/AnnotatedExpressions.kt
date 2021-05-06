fun foo() {

    @Suppress
    foo()

    @Suppress
    val a = 1

    @Suppress
    var b = 2

    @Suppress
    b = a

    @Suppress
    if (a > 2)
        a
    else
        b

    val c = @Suppress a ?: b

}

fun annotatedSwitch(str: String) =
    when {
        @Suppress("DEPRECATION")
        str.isBlank() -> null
        str.isNotEmpty() != null -> null
        else -> 1
    }
