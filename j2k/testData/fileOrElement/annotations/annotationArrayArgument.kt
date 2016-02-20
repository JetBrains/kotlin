import kotlin.reflect.KClass

annotation class Ann(vararg val value: Inner, val test1: Array<InnerParam> = arrayOf(InnerParam(C::class)))

annotation class Inner

annotation class InnerParam(val value: KClass<*>)

@Ann(value = *arrayOf(Inner(), Inner()), test1 = arrayOf(InnerParam(C::class)))
class C

@Ann(Inner(), Inner())
class D

@Ann(value = Inner())
class E

@Ann(value = *arrayOf(Inner()), test1 = arrayOf(InnerParam(value = C::class)))
class F