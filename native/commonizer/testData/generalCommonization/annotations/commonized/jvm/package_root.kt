import kotlin.annotation.AnnotationTarget.*

@Target(ANNOTATION_CLASS)
actual annotation class CommonAnnotationForAnnotationClassesOnly actual constructor(actual val text: String)

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER, TYPE_PARAMETER, FUNCTION, CLASS, CONSTRUCTOR, TYPEALIAS, TYPE)
@JvmAnnotationForAnnotationClassesOnly("annotation-class")
@CommonAnnotationForAnnotationClassesOnly("annotation-class")
actual annotation class CommonAnnotation actual constructor(actual val text: String)

@Target(ANNOTATION_CLASS)
annotation class JvmAnnotationForAnnotationClassesOnly(val text: String)

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER, TYPE_PARAMETER, FUNCTION, CLASS, CONSTRUCTOR, TYPEALIAS, TYPE)
@JvmAnnotationForAnnotationClassesOnly("annotation-class")
@CommonAnnotationForAnnotationClassesOnly("annotation-class")
annotation class JvmAnnotation(val text: String)

//@Target(AnnotationTarget.CLASS)
//actual annotation class CommonOuterAnnotation(actual val inner: CommonInnerAnnotation)
//actual annotation class CommonInnerAnnotation(actual val text: String)
//
//@Target(AnnotationTarget.CLASS)
//annotation class JvmOuterAnnotation(val inner: JvmInnerAnnotation)
//annotation class JvmInnerAnnotation(val text: String)

@JvmAnnotation("property")
@CommonAnnotation("property")
actual var propertyWithoutBackingField
    @JvmAnnotation("getter") @CommonAnnotation("getter") get() = 3.14
    @JvmAnnotation("setter") @CommonAnnotation("setter") set(@JvmAnnotation("parameter") @CommonAnnotation("parameter") value) = Unit

@field:JvmAnnotation("field")
@field:CommonAnnotation("field")
actual val propertyWithBackingField = 3.14

@delegate:JvmAnnotation("field")
@delegate:CommonAnnotation("field")
actual val propertyWithDelegateField: Int by lazy { 42 }

actual val <@JvmAnnotation("type-parameter") @CommonAnnotation("type-parameter") T : CharSequence> @receiver:JvmAnnotation("receiver") @receiver:CommonAnnotation("receiver") T.propertyWithExtensionReceiver: Int
    get() = length

@JvmAnnotation("function")
@CommonAnnotation("function")
actual fun function1(@JvmAnnotation("parameter") @CommonAnnotation("parameter") text: String) = text

@JvmAnnotation("function")
@CommonAnnotation("function")
actual fun <@JvmAnnotation("type-parameter") @CommonAnnotation("type-parameter") Q : Number> @receiver:JvmAnnotation("receiver") @receiver:CommonAnnotation("receiver") Q.function2(): Q = this

@JvmAnnotation("class")
@CommonAnnotation("class")
actual class AnnotatedClass @JvmAnnotation("constructor") @CommonAnnotation("constructor") actual constructor(actual val value: String)

@JvmAnnotation("jvm-only-class")
@CommonAnnotation("jvm-only-class")
class JvmOnlyAnnotatedClass @JvmAnnotation("jvm-only-constructor") @CommonAnnotation("jvm-only-constructor") constructor(val value: String)

@JvmAnnotation("non-lifted-up-type-alias")
@CommonAnnotation("non-lifted-up-type-alias")
actual typealias AnnotatedNonLiftedUpTypeAlias = JvmOnlyAnnotatedClass

//@JvmOuterAnnotation(inner = JvmInnerAnnotation("nested-annotations"))
//@CommonOuterAnnotation(inner = CommonInnerAnnotation("nested-annotations"))
//actual object ObjectWithNestedAnnotations
