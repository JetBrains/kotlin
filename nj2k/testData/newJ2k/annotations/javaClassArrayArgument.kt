import kotlin.reflect.KClass

internal annotation class Ann(vararg val value: KClass<*>)

@Ann(String::class, Any::class)
internal class C

@Ann
internal class D