import kotlin.reflect.KClass

annotation class Ann(vararg val value: Inner, val test1: Array<InnerParam> = [InnerParam(C::class)])

annotation class Inner

annotation class InnerParam(val value: KClass<*>)

@Ann(value = [Inner(), Inner()], test1 = [InnerParam(C::class)])
class C

@Ann(Inner(), Inner())
class D

@Ann(value = [Inner()])
class E

@Ann(value = [Inner()], test1 = [InnerParam(value = C::class)])
class F