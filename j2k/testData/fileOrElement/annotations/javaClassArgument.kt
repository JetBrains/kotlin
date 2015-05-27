import kotlin.reflect.KClass

annotation class Ann(public val value: KClass<*>, public val other: KClass<*>)

Ann(other = String::class, value = Any::class)
class C