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

    assertEquals("Object.Trait.a", Object.Trait.a)
    assertEquals("Object.Trait.b", Object.Trait.b)
    assertEquals(324, Object.Trait.test())

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

    //TODO inner class
//    assertEquals("Class.InnerClass().a", Class().InnerClass("Class.InnerClass().a").a)
//    assertEquals("Class.InnerClass().b", Class().InnerClass("something").b)
//    assertEquals(66, Class().InnerClass("something").test())

    assertEquals("Class.Trait.a", Class.Trait.a)
    assertEquals("Class.Trait.b", Class.Trait.b)
    assertEquals(55, Class.Trait.test())

    assertEquals("Class.a.a", Class.a.a)
    assertEquals("Class.a.b", Class.a.b)
    assertEquals(22, Class.a.test())
    assertEquals("Class.b", Class.b)
    assertEquals(77, Class.test())

    // in trit

    assertEquals("Trait.Object.a", Trait.Object.a)
    assertEquals("Trait.Object.b", Trait.Object.b)
    assertEquals(90, Trait.Object.test())

    assertEquals("Trait.Class().a", Trait.Class("Trait.Class().a").a)
    assertEquals("Trait.Class().b", Trait.Class("something").b)
    assertEquals(66, Trait.Class("something").test())
    assertEquals("Trait.Class.a", Trait.Class.a)
    assertEquals("Trait.Class.b", Trait.Class.b)
    assertEquals(88, Trait.Class.test())

    assertEquals("Trait.Trait.a", Trait.Trait.a)
    assertEquals("Trait.Trait.b", Trait.Trait.b)
    assertEquals(55, Trait.Trait.test())

    assertEquals("Trait.a.a", Trait.a.a)
    assertEquals("Trait.a.b", Trait.a.b)
    assertEquals(22, Trait.a.test())
    assertEquals("Trait.b", Trait.b)
    assertEquals(277, Trait.test())

    return "OK";
}

native
object Object {
    object Object {
        val a: String = noImpl
        var b: String = noImpl
        fun test(): Int = noImpl

        native("AnotherClass")
        class Class(val a: String) {
            var b: String = noImpl
            fun test(): Int = noImpl

            default object {
                val a: String = noImpl
                var b: String = noImpl
                fun test(): Int = noImpl
            }
        }
    }

    class Class(val a: String) {
        var b: String = noImpl
        fun test(): Int = noImpl

        default object {
            val a: String = noImpl
            var b: String = noImpl
            fun test(): Int = noImpl
        }
    }

    trait Trait {
        val a: String
        var b: String
        fun test(): Int = noImpl

        default object {
            val a: String = noImpl
            var b: String = noImpl
            fun test(): Int = noImpl
        }
    }

    val a: Trait = noImpl
    var b: String = noImpl
    fun test(): Int = noImpl
}

native("SomeClass")
class Class {
    object Object {
        val a: String = noImpl
        var b: String = noImpl
        fun test(): Int = noImpl
    }

    class Class(val a: String) {
        var b: String = noImpl
        fun test(): Int = noImpl

        default object {
            val a: String = noImpl
            var b: String = noImpl
            fun test(): Int = noImpl
        }
    }

    inner class InnerClass(val a: String) {
        var b: String = noImpl
        fun test(): Int = noImpl
    }

    trait Trait {
        val a: String
        var b: String
        fun test(): Int = noImpl

        default object {
            val a: String = noImpl
            var b: String = noImpl
            fun test(): Int = noImpl
        }
    }

    default object {
        native("aaa")
        val a: Trait = noImpl
        var b: String = noImpl
        fun test(): Int = noImpl
    }
}

native
trait Trait {
    native("SomeObject")
    object Object {
        val a: String = noImpl
        var b: String = noImpl
        fun test(): Int = noImpl
    }

    class Class(val a: String) {
        var b: String = noImpl
        fun test(): Int = noImpl

        default object {
            val a: String = noImpl
            var b: String = noImpl
            fun test(): Int = noImpl
        }
    }

    native("SomeTrait")
    trait Trait {
        val a: String
        var b: String
        fun test(): Int = noImpl

        default object {
            val a: String = noImpl
            var b: String = noImpl
            fun test(): Int = noImpl
        }
    }

    default object {
        val a: Trait = noImpl
        var b: String = noImpl
        fun test(): Int = noImpl
    }
}
