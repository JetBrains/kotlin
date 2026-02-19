interface Bar0
interface Bar1<T>

sealed interface Foo<in R> {
    public operator fun Bar0.invoke(block: () -> R)
    public operator fun <Q> Bar1<Q>.invoke(block: (Q) -> R)
}