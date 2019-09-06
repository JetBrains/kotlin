import kotlin.annotation.AnnotationTarget.*

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER, TYPE_PARAMETER, FUNCTION)
annotation class Foo(val text: String)

@Foo("property")
var propertyWithoutBackingField
    @Foo("getter") get() = 3.14
    @Foo("setter") set(@Foo("parameter") value) = Unit

@field:Foo("field")
val propertyWithBackingField = 3.14

@delegate:Foo("field")
val propertyWithDelegateField: Int by lazy { 42 }

val <@Foo("type-parameter") T : CharSequence> @receiver:Foo("receiver") T.propertyWithExtensionReceiver: Int
    get() = length

@Foo("function")
fun function1(@Foo("parameter") text: String) = text

@Foo("function")
fun <@Foo("type-parameter") Q : Number> @receiver:Foo("receiver") Q.function2() = this
