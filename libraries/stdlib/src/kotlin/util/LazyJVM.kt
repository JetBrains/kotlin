package kotlin


/** Initializes a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and the default thread-safety [LazyThreadSafetyMode.SYNCHRONIZED]. */
public fun lazy<T>(initializer: () -> T): Lazy<T> = LazyImpl(initializer, Any())

/** Initializes a new instance of the [Lazy] that uses the specified initialization function [initializer]
 * and thread-safety [mode]. */
public fun lazy<T>(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T> =
    when (mode) {
        LazyThreadSafetyMode.SYNCHRONIZED -> LazyImpl(initializer, Any())
        LazyThreadSafetyMode.NONE -> UnsafeLazyImpl(initializer)
    }
