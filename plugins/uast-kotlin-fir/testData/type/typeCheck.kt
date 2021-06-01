fun Any?.asString(): String {
    return if (x !is String)
        x.toString()
    else
        x
}
