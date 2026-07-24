package test

fun compute(): Int {
    val collector = object : Collector<Int> {
        override fun emit(value: Int) {}
    }
    val list = collector.toCollection(mutableListOf())
    return identity(list.size + 42)
}
