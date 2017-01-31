package foo

fun box(): String {
    // in object

    assertEquals("Object.Object.a", Object.Object.a)
    assertEquals("Object.Object.b", Object.Object.b)
    assertEquals(123, Object.Object.test())

    assertEquals("Object.Object.Class().a", Object.Object.Class("Object.Object.Class().a").a)
    assertEquals("Object.Object.Class().b", Object.Object.Class("something").b)
    assertEquals(42, Object.Object.Class("something").test())
    assertEquals("Object.Object.Class.a", Object.Object.Class.a)
    assertEquals("Object.Object.Class.b", Object.Object.Class.b)
    assertEquals(142, Object.Object.Class.test())

    assertEquals("Object.Class().a", Object.Class("Object.Class().a").a)
    assertEquals("Object.Class().b", Object.Class("something").b)
    assertEquals(42, Object.Class("something").test())
    assertEquals("Object.Class.a", Object.Class.a)
    assertEquals("Object.Class.b", Object.Class.b)
    assertEquals(142, Object.Class.test())

    assertEquals("Object.a.a", Object.a.a)
    assertEquals("Object.a.b", Object.a.b)
    assertEquals(34, Object.a.test())
    assertEquals("Object.b", Object.b)
    assertEquals(23, Object.test())

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

external object Object {
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
