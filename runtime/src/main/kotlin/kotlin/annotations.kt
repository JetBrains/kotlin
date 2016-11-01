package kotlin

// TODO: shouldn't these annotation be located in 'kotlin_native.internal' package?

/**
 * Forces the compiler to use specified symbol name for the target `external` function.
 *
 * TODO: changing symbol name breaks the binary compatibility,
 * so it should probably be allowed on `internal` and `private` functions only.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class SymbolName(val name: kotlin.String)

/**
 * Exports the TypeInfo of this class by given name to use it from runtime.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ExportTypeInfo(val name: kotlin.String)
