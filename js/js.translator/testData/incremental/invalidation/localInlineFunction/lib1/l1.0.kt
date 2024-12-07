fun foo(): String {
    inline fun localFoo(): String {
        return "OK-0"
    }

    return localFoo()
}
