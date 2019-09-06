import kotlin.annotation.AnnotationTarget.*

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER)
annotation class Bar(val text: String)

@Bar("property")
actual var propertyWithoutBackingField
    @Bar("getter") get() = 3.14
    @Bar("setter") set(@Bar("parameter") value) = Unit

@field:Bar("field")
actual val propertyWithBackingField = 3.14

@delegate:Bar("field")
actual val propertyWithDelegateField: Int by lazy { 42 }

actual val <@Bar("type-parameter") T : CharSequence> @receiver:Bar("receiver") T.propertyWithExtensionReceiver: Int
    get() = length

@Bar("function")
actual fun function1(@Bar("parameter") text: String) = text

@Bar("function")
actual fun <@Bar("type-parameter") Q : Number> @receiver:Bar("receiver") Q.function2() = this
