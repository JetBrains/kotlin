package inlibrary.test

public fun topLevel(): Int {
    fun local(): Int {
        return 1
    }

    return local()
}