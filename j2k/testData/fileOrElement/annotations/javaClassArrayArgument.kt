import kotlin.reflect.KClass

annotation internal class Ann(vararg val value: KClass<*>)

Ann(String::class, Any::class)
internal class C

Ann
internal class D