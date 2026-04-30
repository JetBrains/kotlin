package java.util.function

public interface Consumer<T> {
    public fun accept(t: T)
}

public interface BiConsumer<T, U> {
    public fun accept(t: T, u: U)
}

public interface Predicate<T> {
    public fun test(t: T): Boolean
}

public interface Function<T, R> {
    public fun apply(t: T): R
}

public interface BiFunction<T, U, R> {
    public fun apply(t: T, u: U): R
}

public interface UnaryOperator<T> : Function<T, T>
