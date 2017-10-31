// CORRECT_ERROR_TYPES
// NON_EXISTENT_CLASS
// NO_VALIDATION
// WITH_RUNTIME

import java.util.Calendar
import kotlin.reflect.KClass

@Suppress("UNRESOLVED_REFERENCE", "ANNOTATION_PARAMETER_MUST_BE_CONST", "NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION", "UNSUPPORTED_FEATURE")
@Anno(Blah::class, arrayOf(NoFoo1::class, NoBar1::class), [NoFoo2::class, String::class], Boolean::class, NoBar3::class)
class Test {
    lateinit var a: ABC
    val b: ABC? = null
    val c: List<ABC>? = null
    val d: List<Map<BCD, ABC<List<BCD>>>>? = null
    lateinit var e: List<out Map<out ABC, out BCD>?>
    lateinit var f: ABC<*>
    lateinit var g: List<*>
    lateinit var h: ABC<Int, String>
    lateinit var i: (ABC, List<BCD>) -> CDE
    lateinit var j: () -> CDE
    lateinit var k: ABC.(List<BCD>) -> CDE

    lateinit var l: ABC.BCD.EFG

    val m = ABC()
    val n = "".toString()

    lateinit var o11: List<List<List<List<List<List<List<List<List<List<ABC>>>>>>>>>>
    lateinit var o10: List<List<List<List<List<List<List<List<List<ABC>>>>>>>>>

    lateinit var p: Calendar.Builder

    fun f1(a: ABC): BCD? {
        return null
    }

    fun <T> f2(a: ABC<String, Int, () -> BCD>) {}

    fun <T> f3(a: ABC, b: Int): Long {
        return 0
    }

    fun f4() = ABC()

    fun <T> MyType<T>.f5(): java.lang.Class<Enum<*>>? = null
}

class MyType<T>

annotation class Anno(val a: KClass<*>, val b: Array<KClass<*>>, val c: Array<KClass<*>>, vararg val d: KClass<*>)