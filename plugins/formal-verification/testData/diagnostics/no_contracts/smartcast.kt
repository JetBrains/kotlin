fun <!VIPER_TEXT!>smartcastReturn<!>(n: Int?): Int =
    if (n != null) n else 0

fun <!VIPER_TEXT!>isNullOrEmptyWrong<!>(seq: CharSequence?): Boolean =
    seq == null && seq?.length == 0
