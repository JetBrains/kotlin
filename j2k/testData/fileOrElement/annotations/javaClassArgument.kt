import kotlin.reflect.KClass

annotation internal class Ann(val value: KClass<*>, val other: KClass<*>)

Ann(other = String::class, value = Any::class)
internal class C