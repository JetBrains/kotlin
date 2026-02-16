// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: objects.kt

package foo

@JsExport
interface Interface1 {
    fun foo(): String
}
@JsExport
interface Interface2 {
    fun bar(): String
}

@JsExport
object O0

@JsExport
object O {
    val x = 10
    fun foo() = 20
}

@JsExport
fun takesO(o: O): Int =
    O.x + O.foo()

@JsExport
object WithSimpleObjectInside {
   val value: String = "WithSimpleObjectInside"
    object SimpleObject {
        val value: String = "SimpleObject"
    }
}

@JsExport
object Parent {
    object Nested1 {
        val value: String = "Nested1"
        class Nested2 {
            companion object {
                class Nested3
            }
        }
    }
}

@JsExport
fun getParent(): Parent {
    return Parent
}

@JsExport
fun createNested1(): Parent.Nested1 {
    return Parent.Nested1
}

@JsExport
fun createNested2(): Parent.Nested1.Nested2 {
    return Parent.Nested1.Nested2()
}

@JsExport
fun createNested3(): Parent.Nested1.Nested2.Companion.Nested3 {
    return Parent.Nested1.Nested2.Companion.Nested3()
}

@JsExport
abstract class BaseWithCompanion {
    companion object {
        val any: String = "ANYTHING"
    }
}
@JsExport
class ChildWithCompanion : BaseWithCompanion() {
    companion object
}

@JsExport
object SimpleObjectWithInterface1 : Interface1 {
    override fun foo(): String = "OK"
}

@JsExport
object SimpleObjectWithBothInterfaces : Interface1, Interface2 {
    override fun foo(): String = "OK"
    override fun bar(): String = "OK"
}

@JsExport
object SimpleObjectInheritingAbstract : BaseWithCompanion()

@JsExport
object SimpleObjectInheritingAbstractAndInterface1 : BaseWithCompanion(), Interface1 {
    override fun foo(): String = "OK"
}

@JsExport
object SimpleObjectInheritingAbstractAndBothInterfaces : BaseWithCompanion(), Interface1, Interface2 {
    override fun foo(): String = "OK"
    override fun bar(): String = "OK"
}

@JsExport
object SimpleObjectWithInterface1AndNested : Interface1 {
    override fun foo(): String = "OK"
    class Nested
}

@JsExport
object SimpleObjectWithBothInterfacesAndNested : Interface1, Interface2 {
    override fun foo(): String = "OK"
    override fun bar(): String = "OK"
    class Nested
}

@JsExport
object SimpleObjectInheritingAbstractAndNested : BaseWithCompanion() {
    class Nested
}

@JsExport
object SimpleObjectInheritingAbstractAndInterface1AndNested : BaseWithCompanion(), Interface1 {
    override fun foo(): String = "OK"
    class Nested
}

@JsExport
object SimpleObjectInheritingAbstractAndBothInterfacesAndNested : BaseWithCompanion(), Interface1, Interface2 {
    override fun foo(): String = "OK"
    override fun bar(): String = "OK"
    class Nested
}


@JsExport
abstract class Money<T : Money<T, Array<T>>, I: Array<T>> protected constructor() {
    abstract val amount: Float
    fun isZero(): Boolean = amount == 0f
}

@JsExport
object Zero : Money<Zero, Array<Zero>>() {
    override val amount = 0f
}

@JsExport
abstract class AbstractClassWithProtected {
    protected abstract fun protectedAbstractFun(): Int
    protected abstract val protectedAbstractVal: Int
}

@JsExport
object ObjectWithProtected : AbstractClassWithProtected() {
    override fun protectedAbstractFun(): Int = 42
    override val protectedAbstractVal: Int
        get() = 42
}
