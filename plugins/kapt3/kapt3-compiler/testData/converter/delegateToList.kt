// FIR_BLOCKED: KT-60480
class MyList<out T : Any>(
    private val wrappedList: List<T>,
) : List<T> by wrappedList
