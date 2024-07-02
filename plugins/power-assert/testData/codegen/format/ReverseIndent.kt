fun box() = expectThrowableMessage {
    val text: String? = "Hello"
    /* do not print  */ assert(
                    text
                == null || // Intentional blank line

            (
        text.length == 5 &&
    text.toLowerCase() == text
        )
                        )
}
