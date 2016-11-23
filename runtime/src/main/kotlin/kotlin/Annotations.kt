package kotlin

// TODO: shouldn't these annotation be located in 'kotlin_native.internal' package?

/**
 * Forces the compiler to use specified symbol name for the target `external` function.
 *
 * TODO: changing symbol name breaks the binary compatibility,
 * so it should probably be allowed on `internal` and `private` functions only.
 */
//@Target(AnnotationTarget.FUNCTION)
//@Retention(AnnotationRetention.SOURCE)
annotation class SymbolName(val name: String)

/**
 * Exports the TypeInfo of this class by given name to use it from runtime.
 */
//@Target(AnnotationTarget.CLASS)
//@Retention(AnnotationRetention.SOURCE)
annotation class ExportTypeInfo(val name: String)

/**
 * Suppresses the given compilation warnings in the annotated element.
 * @property names names of the compiler diagnostics to suppress.
 */
//@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER,
//        CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPE, EXPRESSION, FILE, TYPEALIAS)
//@Retention(SOURCE)
public annotation class Suppress(vararg val names: String)

/**
 * Suppresses errors about variance conflict
 */
@Target(AnnotationTarget.TYPE)
//@Retention(SOURCE)
//@MustBeDocumented
public annotation class UnsafeVariance