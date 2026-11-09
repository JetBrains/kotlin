interface Callback {
    fun produce(): String
}

object Caller {
    fun useCallback(c: Callback): String = c.produce()
}
