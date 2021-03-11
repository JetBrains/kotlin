package dependency

public class Foo<T> {
}

public class FooIterator<T> {
}

public fun <T> Foo<T>.iterator(): FooIterator<T> = FooIterator<T>()
public fun <T> FooIterator<T>.hasNext(): Boolean = false
public fun <T> FooIterator<T>.next(): T = throw IllegalStateException()
