package kotlin.internal


/**
 * The value of this type parameter should be mentioned in input types (argument types, receiver type or expected type).
 */
@Suppress("HEADER_WITHOUT_IMPLEMENTATION")
@Target(AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
internal header annotation class OnlyInputTypes

/**
 * Specifies that this function should not be called directly without inlining
 */
@Suppress("HEADER_WITHOUT_IMPLEMENTATION")
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal header annotation class InlineOnly
