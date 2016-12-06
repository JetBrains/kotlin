package kotlin

/**
 * Suppresses the given compilation warnings in the annotated element.
 * @property names names of the compiler diagnostics to suppress.
 */
//@Target(CLASS, ANNOTATION_CLASS, PROPERTY, FIELD, LOCAL_VARIABLE, VALUE_PARAMETER,
//        CONSTRUCTOR, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, TYPE, EXPRESSION, FILE, TYPEALIAS)
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
//@Retention(SOURCE)
public annotation class Suppress(vararg val names: String)

/**
 * Suppresses errors about variance conflict
 */
@Target(AnnotationTarget.TYPE)
//@Retention(SOURCE)
//@MustBeDocumented
public annotation class UnsafeVariance

