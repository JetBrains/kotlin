class MyList<out T : Any>(
    private val wrappedList: List<T>,
) : List<T> by wrappedList
