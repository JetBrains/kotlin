import kotlin.reflect.KClass

annotation class Ann(public vararg val value: KClass<*>)

Ann(String::class, Any::class)
class C

Ann
class D
