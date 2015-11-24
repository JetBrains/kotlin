package kotlin.jvm


/**
 * Specifies the name for the Java class or method
 * which is generated from this element.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/java-interop.html#handling-signature-clashes-with-platformname)
 * for more information.
 * @property name the name of the element.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class JvmName(public val name: String)

/**
 * Instructs the Kotlin compiler to generate a multifile class with top-level functions and properties declared in this file as one of its parts.
 * Name of the corresponding multifile class is provided by the [JvmName] annotation.
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
public annotation class JvmMultifileClass


