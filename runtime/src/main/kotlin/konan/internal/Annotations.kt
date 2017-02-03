package konan.internal

/**
 * Makes this function to be possible to call by given name from C++ part of runtime using C ABI.
 * The parameters are mapped in an implementation-dependent manner.
 *
 * The function to call from C++ can be a wrapper around the original function.
 *
 * If the name is not specified, the function to call will be available by its Kotlin unqualified name.
 */
//@Target(AnnotationTarget.FUNCTION)
//@Retention(AnnotationRetention.SOURCE)
annotation class ExportForCppRuntime(val name: String = "")

// This one is used internally to mark the presence of a backing field
// in the absence of IR.
annotation class HasBackingField

/**
 * This annotation denotes that the element is intrinsic and its usages require special handling in compiler.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Intrinsic

/**
 * Exports symbol for compiler needs.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ExportForCompiler