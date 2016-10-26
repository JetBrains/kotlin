annotation class Anno1
enum class Colors { WHITE, BLACK }
annotation class Anno2(
        val i: Int = 5,
        val s: String = "ABC",
        val ii: IntArray = intArrayOf(1, 2, 3),
        val ss: Array<String> = arrayOf("A", "B"),
        val a: Anno1,
        val color: Colors = Colors.BLACK,
        val colors: Array<Colors> = arrayOf(Colors.BLACK, Colors.WHITE),
        val clazz: kotlin.reflect.KClass<*>,
        val classes: Array<kotlin.reflect.KClass<*>>
)
annotation class Anno3(val value: String)

@Anno1
@Anno2(a = Anno1(), clazz = TestAnno::class, classes = arrayOf(TestAnno::class, Anno1::class))
@Anno3(value = "value")
class TestAnno

@Anno3("value")
@Anno2(i = 6, s = "BCD", ii = intArrayOf(4, 5, 6), ss = arrayOf("Z", "X"),
       a = Anno1(), color = Colors.WHITE, colors = arrayOf(Colors.WHITE),
       clazz = TestAnno::class, classes = arrayOf(TestAnno::class, Anno1::class))
class TestAnno2 {
    @Anno1
    fun a(@Anno3("param-pam-pam") param: String) {}

    @get:Anno3("getter") @set:Anno3("setter") @property:Anno3("property") @field:Anno3("field") @setparam:Anno3("setparam")
    var b: String = "property initializer"
}