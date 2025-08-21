// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: objects.kt

@file:JsExport

package foo


interface Interface1 {
    fun foo(): String
}

interface Interface2 {
    fun bar(): String
}


object O0


object O {
    val x = 10
    fun foo() = 20
}


fun takesO(o: O): Int =
    O.x + O.foo()


object WithSimpleObjectInside {
   val value: String = "WithSimpleObjectInside"
    object SimpleObject {
        val value: String = "SimpleObject"
    }
}


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


fun getParent(): Parent {
    return Parent
}


fun createNested1(): Parent.Nested1 {
    return Parent.Nested1
}


fun createNested2(): Parent.Nested1.Nested2 {
    return Parent.Nested1.Nested2()
}


fun createNested3(): Parent.Nested1.Nested2.Companion.Nested3 {
    return Parent.Nested1.Nested2.Companion.Nested3()
}


abstract class BaseWithCompanion {
    companion object {
        val any: String = "ANYTHING"
    }
}

class ChildWithCompanion : BaseWithCompanion() {
    companion object
}


object SimpleObjectWithInterface1 : Interface1 {
    override fun foo(): String = "OK"
}


object SimpleObjectWithBothInterfaces : Interface1, Interface2 {
    override fun foo(): String = "OK"
    override fun bar(): String = "OK"
}


object SimpleObjectInheritingAbstract : BaseWithCompanion()


object SimpleObjectInheritingAbstractAndInterface1 : BaseWithCompanion(), Interface1 {
    override fun foo(): String = "OK"
}


object SimpleObjectInheritingAbstractAndBothInterfaces : BaseWithCompanion(), Interface1, Interface2 {
    override fun foo(): String = "OK"
    override fun bar(): String = "OK"
}


object SimpleObjectWithInterface1AndNested : Interface1 {
    override fun foo(): String = "OK"
    class Nested
}


object SimpleObjectWithBothInterfacesAndNested : Interface1, Interface2 {
    override fun foo(): String = "OK"
    override fun bar(): String = "OK"
    class Nested
}


object SimpleObjectInheritingAbstractAndNested : BaseWithCompanion() {
    class Nested
}


object SimpleObjectInheritingAbstractAndInterface1AndNested : BaseWithCompanion(), Interface1 {
    override fun foo(): String = "OK"
    class Nested
}


object SimpleObjectInheritingAbstractAndBothInterfacesAndNested : BaseWithCompanion(), Interface1, Interface2 {
    override fun foo(): String = "OK"
    override fun bar(): String = "OK"
    class Nested
}



abstract class Money<T : Money<T, Array<T>>, I: Array<T>> protected constructor() {
    abstract val amount: Float
    fun isZero(): Boolean = amount == 0f
}


object Zero : Money<Zero, Array<Zero>>() {
    override val amount = 0f
}