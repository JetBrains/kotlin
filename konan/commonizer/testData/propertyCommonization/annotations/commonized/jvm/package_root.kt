import kotlin.annotation.AnnotationTarget.*

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER)
@Repeatable
annotation class Bar(val text: String)

@Bar("property")
actual var propertyWithoutBackingField
    @Bar("getter") get() = 3.14
    @Bar("setter") set(@Bar("parameter") value) = Unit

@field:Bar("field")
actual val propertyWithBackingField = 3.14

@delegate:Bar("field")
actual val propertyWithDelegateField: Int by lazy { 42 }

actual val @receiver:Bar("receiver") String.propertyWithExtensionReceiver: Int
    get() = length
