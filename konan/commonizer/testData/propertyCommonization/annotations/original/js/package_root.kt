import kotlin.annotation.AnnotationTarget.*

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER)
@Repeatable
annotation class Foo(val text: String)

@Foo("property")
var propertyWithoutBackingField
    @Foo("getter") get() = 3.14
    @Foo("setter") set(@Foo("parameter") value) = Unit

@field:Foo("field")
val propertyWithBackingField = 3.14

@delegate:Foo("field")
val propertyWithDelegateField: Int by lazy { 42 }

val @receiver:Foo("receiver") String.propertyWithExtensionReceiver: Int
    get() = length
