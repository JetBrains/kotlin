import kotlin.reflect.KClass

annotation class Anno(val value: KClass<*>)

@Anno(value = T::class)
object O

typealias T = A

class A
