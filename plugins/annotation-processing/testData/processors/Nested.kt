import kotlin.reflect.KClass

annotation class Anno(val a: Anno2, val b: Array<Anno2>, val c: String, val d: KClass<*>, val e: Array<KClass<*>>)
annotation class Anno2(val name: String)

@Anno(a = Anno2("Tim"), b = arrayOf(Anno2("Kate"), Anno2("Mary")), c = "ABC", d = Anno2::class, e = arrayOf(Anno::class))
class Test