fun foo(): String {
    inline fun localFoo(): String {
        return "OK-1"
    }

    return localFoo()
}
