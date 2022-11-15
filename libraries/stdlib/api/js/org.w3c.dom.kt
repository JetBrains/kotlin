public fun <T> org.w3c.dom.ItemArrayLike<T>.asList(): kotlin.collections.List<T>

public external interface ItemArrayLike<out T> {
    public abstract val length: kotlin.Int { get; }

    public abstract fun item(index: kotlin.Int): T?
}