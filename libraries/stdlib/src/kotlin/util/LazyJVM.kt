package kotlin


public fun lazy<T>(initializer: () -> T): Lazy<T> = LazyImpl(initializer, Any())
public fun lazy<T>(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> =
    when (mode) {
        LazyThreadSafetyMode.SYNCHRONIZED -> LazyImpl(initializer, Any())
        LazyThreadSafetyMode.NONE -> UnsafeLazyImpl(initializer)
    }
