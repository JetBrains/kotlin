package test

fun compute(): Int {
    val collector = object : Collector<Int> {
        override fun emit(value: Int) {}
    }
    val grouped = collector.groupBy { it % 2 }
    val assoc = collector.associateWith { it * 10 }
    return identity(grouped.size + assoc.size + 42)
}
