import kotlin.annotation.AnnotationTarget.*

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER)
annotation class Bar(val text: String)

@Bar("property")
var propertyWithoutBackingField
    @Bar("getter") get() = 3.14
    @Bar("setter") set(@Bar("parameter") value) = Unit

@field:Bar("field")
val propertyWithBackingField = 3.14

@delegate:Bar("field")
val propertyWithDelegateField: Int by lazy { 42 }

val <@Bar("type-parameter") T : CharSequence> @receiver:Bar("receiver") T.propertyWithExtensionReceiver: Int
    get() = length

@Bar("function")
fun function1(@Bar("parameter") text: String) = text

@Bar("function")
fun <@Bar("type-parameter") Q : Number> @receiver:Bar("receiver") Q.function2() = this
