import kotlin.annotation.AnnotationTarget.*

@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, FIELD, VALUE_PARAMETER)
@Repeatable
annotation class Foo(val text: String)

@property:Foo("property")
@get:Foo("getter")
@set:Foo("setter")
@setparam:Foo("parameter")
actual var propertyWithoutBackingField
    get() = 3.14
    set(value) = Unit

@field:Foo("field")
actual val propertyWithBackingField = 3.14

@delegate:Foo("field")
actual val propertyWithDelegateField: Int by lazy { 42 }

actual val @receiver:Foo("receiver") String.propertyWithExtensionReceiver: Int
    get() = length
