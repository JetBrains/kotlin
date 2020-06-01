import kotlin.annotation.AnnotationTarget.*

@Target(ANNOTATION_CLASS)
actual annotation class CommonAnnotationForAnnotationClassesOnly actual constructor(actual val text: String)

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER, TYPE_PARAMETER, FUNCTION, CLASS, CONSTRUCTOR, TYPEALIAS, TYPE)
@JsAnnotationForAnnotationClassesOnly("annotation-class")
@CommonAnnotationForAnnotationClassesOnly("annotation-class")
actual annotation class CommonAnnotation actual constructor(actual val text: String)

@Target(ANNOTATION_CLASS)
annotation class JsAnnotationForAnnotationClassesOnly(val text: String)

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER, TYPE_PARAMETER, FUNCTION, CLASS, CONSTRUCTOR, TYPEALIAS, TYPE)
@JsAnnotationForAnnotationClassesOnly("annotation-class")
@CommonAnnotationForAnnotationClassesOnly("annotation-class")
annotation class JsAnnotation(val text: String)

//@Target(AnnotationTarget.CLASS)
//actual annotation class CommonOuterAnnotation(actual val inner: CommonInnerAnnotation)
//actual annotation class CommonInnerAnnotation(actual val text: String)

//@Target(AnnotationTarget.CLASS)
//annotation class JsOuterAnnotation(val inner: JsInnerAnnotation)
//annotation class JsInnerAnnotation(val text: String)

@JsAnnotation("property")
@CommonAnnotation("property")
actual var propertyWithoutBackingField
    @JsAnnotation("getter") @CommonAnnotation("getter") get() = 3.14
    @JsAnnotation("setter") @CommonAnnotation("setter") set(@JsAnnotation("parameter") @CommonAnnotation("parameter") value) = Unit

@field:JsAnnotation("field")
@field:CommonAnnotation("field")
actual val propertyWithBackingField = 3.14

@delegate:JsAnnotation("field")
@delegate:CommonAnnotation("field")
actual val propertyWithDelegateField: Int by lazy { 42 }

actual val <@JsAnnotation("type-parameter") @CommonAnnotation("type-parameter") T : CharSequence> @receiver:JsAnnotation("receiver") @receiver:CommonAnnotation("receiver") T.propertyWithExtensionReceiver: Int
    get() = length

@JsAnnotation("function")
@CommonAnnotation("function")
actual fun function1(@JsAnnotation("parameter") @CommonAnnotation("parameter") text: String) = text

@JsAnnotation("function")
@CommonAnnotation("function")
actual fun <@JsAnnotation("type-parameter") @CommonAnnotation("type-parameter") Q : Number> @receiver:JsAnnotation("receiver") @receiver:CommonAnnotation("receiver") Q.function2(): Q = this

@JsAnnotation("class")
@CommonAnnotation("class")
actual class AnnotatedClass @JsAnnotation("constructor") @CommonAnnotation("constructor") actual constructor(actual val value: String)

@JsAnnotation("js-only-class")
@CommonAnnotation("js-only-class")
class JsOnlyAnnotatedClass @JsAnnotation("js-only-constructor") @CommonAnnotation("js-only-constructor") constructor(val value: String)

@JsAnnotation("non-lifted-up-type-alias")
@CommonAnnotation("non-lifted-up-type-alias")
actual typealias AnnotatedNonLiftedUpTypeAlias = JsOnlyAnnotatedClass

//@JsOuterAnnotation(inner = JsInnerAnnotation("nested-annotations"))
//@CommonOuterAnnotation(inner = CommonInnerAnnotation("nested-annotations"))
//actual object ObjectWithNestedAnnotations
