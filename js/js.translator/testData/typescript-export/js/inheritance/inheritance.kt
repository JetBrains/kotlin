// IGNORE_BACKEND: JS_IR_ES6
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: inheritance.kt

package foo

@JsExport
external interface I<T, out S, in U> {
    var x: T
    val y: S
    fun z(u: U)
}

@JsExport
external interface I2 {
    var x: String
    val y: Boolean
    fun z(z: Int)
}

@JsExport
abstract class AC : I2 {
    override var x = "AC"
    override abstract val y: Boolean
    override abstract fun z(z: Int)

    val acProp: String = "acProp"
    abstract val acAbstractProp: String
}

@JsExport
open class OC(
    override val y: Boolean,
    override val acAbstractProp: String
) : AC(), I<String, Boolean, Int> {
    override fun z(z: Int) {
    }

    private val privateX: String = "privateX"
    private fun privateFun(): String = "privateFun"
}

@JsExport
final class FC : OC(true, "FC")

@JsExport
object O1 : OC(true, "O1")

@JsExport
object O2 : OC(true, "O2") {
    @JsName("foo")  // TODO: Should work without JsName
    fun foo(): Int = 10
}

@JsExport
interface I3 {
    val foo: String
    var bar: String
    val baz: String

    fun bay(): String
}

@JsExport
fun getI3(): I3 = object : I3 {
    override val foo: String = "fooI3"

    override var bar: String = "barI3"

    override var baz: String = "bazI3"

    override fun bay(): String = "bayI3"
}

abstract class A : I3

@JsExport
fun getA(): I3 = object : A() {
    override val foo: String = "fooA"

    override var bar: String = "barA"

    override var baz: String = "bazA"

    override fun bay(): String = "bayA"
}

open class B : A() {
    override val foo: String = "fooB"

    override var bar: String = "barB"

    override val baz: String = "bazB"

    override fun bay(): String = "bayB"
}

@JsExport
fun getB(): I3 = B()

open class C : B() {
    override val foo: String = "fooC"

    override var bar: String = "barC"

    override var baz: String = "bazC"

    override fun bay(): String = "bayC"
}

@JsExport
fun getC(): I3 = C()

@JsExport
abstract class A2 : I3

@JsExport
open class B2 : A2() {
    override val foo: String = "fooB2"

    override var bar: String = "barB2"

    override val baz: String = "bazB2"

    override fun bay(): String = "bayB2"
}

@JsExport
open class C2 : B2() {
    override val foo: String = "fooC2"

    override var bar: String = "barC2"

    override var baz: String = "bazC2"

    override fun bay(): String = "bayC2"
}

@JsExport
enum class EC : I3 {
    EC1 {
        override var baz = "ec1"

        val bah = "bah"

        fun huh() = "huh"
    },
    EC2 {
        override var baz = "ec2"
    },
    EC3 {
        override var baz = "ec3"
    };

    override val foo: String = "foo"

    override var bar = "bar"

    override fun bay(): String = "bay"
}

// Save hierarhy

@JsExport
interface IA {
    val foo: Any
}

@JsExport
interface IG<T> {
    fun process(value: T): Unit
}

interface IB : IA

interface IC : IB {
    override val foo: Any
}

interface ID : IC {
    override val foo: Int
}

@JsExport
open class Third<T>: Second()

open class Forth<A>: Third<A>(), IB, ID {
    override val foo: Int = 42
}

open class Fifth<B>: Forth<B>(), IG<B> {
    override fun process(value: B) {}
}

@JsExport
class Sixth: Fifth<Int>(), IC

@JsExport
open class First

open class Second: First()

@JsExport
fun <T : Forth<String>> acceptForthLike(forth: T) {}

@JsExport
fun <T> acceptMoreGenericForthLike(forth: T) where T: IB, T: IC, T: Second {}

@JsExport
val fifth = Fifth<Boolean>()
