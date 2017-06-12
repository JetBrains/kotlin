// EXPECTED_REACHABLE_NODES: 519
package foo

fun box(): String {
    // in object

    assertEquals("MyObject.Object.a", MyObject.Object.a)
    assertEquals("MyObject.Object.b", MyObject.Object.b)
    assertEquals(123, MyObject.Object.test())

    assertEquals("MyObject.Object.Class().a", MyObject.Object.Class("MyObject.Object.Class().a").a)
    assertEquals("MyObject.Object.Class().b", MyObject.Object.Class("something").b)
    assertEquals(42, MyObject.Object.Class("something").test())
    assertEquals("MyObject.Object.Class.a", MyObject.Object.Class.a)
    assertEquals("MyObject.Object.Class.b", MyObject.Object.Class.b)
    assertEquals(142, MyObject.Object.Class.test())

    assertEquals("MyObject.Class().a", MyObject.Class("MyObject.Class().a").a)
    assertEquals("MyObject.Class().b", MyObject.Class("something").b)
    assertEquals(42, MyObject.Class("something").test())
    assertEquals("MyObject.Class.a", MyObject.Class.a)
    assertEquals("MyObject.Class.b", MyObject.Class.b)
    assertEquals(142, MyObject.Class.test())

    assertEquals("MyObject.a.a", MyObject.a.a)
    assertEquals("MyObject.a.b", MyObject.a.b)
    assertEquals(34, MyObject.a.test())
    assertEquals("MyObject.b", MyObject.b)
    assertEquals(23, MyObject.test())

    // in class

    assertEquals("Class.Object.a", Class.Object.a)
    assertEquals("Class.Object.b", Class.Object.b)
    assertEquals(55, Class.Object.test())

    assertEquals("Class.Class().a", Class.Class("Class.Class().a").a)
    assertEquals("Class.Class().b", Class.Class("something").b)
    assertEquals(66, Class.Class("something").test())
    assertEquals("Class.Class.a", Class.Class.a)
    assertEquals("Class.Class.b", Class.Class.b)
    assertEquals(88, Class.Class.test())

    assertEquals("Class.a.a", Class.a.a)
    assertEquals("Class.a.b", Class.a.b)
    assertEquals(22, Class.a.test())
    assertEquals("Class.b", Class.b)
    assertEquals(77, Class.test())

    return "OK";
}

external object MyObject {
    object Object {
        val a: String = definedExternally
        var b: String = definedExternally
        fun test(): Int = definedExternally

        @JsName("AnotherClass")
        class Class(a: String) {
            val a: String
            var b: String = definedExternally
            fun test(): Int = definedExternally

            companion object {
                val a: String = definedExternally
                var b: String = definedExternally
                fun test(): Int = definedExternally
            }
        }
    }

    class Class(a: String) {
        val a: String
        var b: String = definedExternally
        fun test(): Int = definedExternally

        companion object {
            val a: String = definedExternally
            var b: String = definedExternally
            fun test(): Int = definedExternally
        }
    }

    interface Trait {
        val a: String
        var b: String
        fun test(): Int
    }

    val a: Trait = definedExternally
    var b: String = definedExternally
    fun test(): Int = definedExternally
}

@JsName("SomeClass")
external class Class {
    object Object {
        val a: String = definedExternally
        var b: String = definedExternally
        fun test(): Int = definedExternally
    }

    class Class(a: String) {
        val a: String
        var b: String = definedExternally
        fun test(): Int = definedExternally

        companion object {
            val a: String = definedExternally
            var b: String = definedExternally
            fun test(): Int = definedExternally
        }
    }

    interface Trait {
        val a: String
        var b: String
        fun test(): Int
    }

    companion object {
        @JsName("aaa")
        val a: Trait = definedExternally
        var b: String = definedExternally
        fun test(): Int = definedExternally
    }
}
