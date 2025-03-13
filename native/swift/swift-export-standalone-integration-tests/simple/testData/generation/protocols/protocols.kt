// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

object MyObject

interface Foeble {
    fun bar(arg: Foeble): Foeble
    val baz: Foeble
}

interface Barable: Foeble {
    override fun bar(arg: Foeble): Barable
    override val baz: Foeble
}

interface Bazzable

class Foo: Foeble {
    override fun bar(arg: Foeble): Foo = this
    override val baz: Foeble get() = this
}

class Bar: Barable, Foeble, Bazzable {
    override fun bar(arg: Foeble): Bar = this
    override val baz: Bar get() = this
}

// FILE: less_trivial.kt

interface ContainerProtocol {
    open class NestedClass

    interface NestedProtocol {
        open class NestedClass
    }
}

interface SiblingProtocol {
    class NestedClass {
        class NestedClass
    }
}

fun ContainerProtocol.foo(): Unit { TODO() }
fun ContainerProtocol.NestedProtocol.NestedClass.foo(): Unit { TODO() }
fun SiblingProtocol.NestedClass.foo(): Unit { TODO() }
fun ContainerProtocol.NestedProtocol.foo(): Unit { TODO() }

// FILE: packaged.kt

package packagewithprotocols

interface ContainerProtocol {
    open class NestedClass

    interface NestedProtocol {
        open class NestedClass
    }
}

interface SiblingProtocol {
    class NestedClass {
        class NestedClass
    }
}

fun ContainerProtocol.foo(): Unit { TODO() }
fun ContainerProtocol.NestedProtocol.NestedClass.foo(): Unit { TODO() }
fun SiblingProtocol.NestedClass.foo(): Unit { TODO() }
fun ContainerProtocol.NestedProtocol.foo(): Unit { TODO() }

// FIXME: See the commend above on ContainerProtocol.NestedClass

class INHERITANCE_COUPLE : ContainerProtocol.NestedClass(), ContainerProtocol
class INHERITANCE_SINGLE_PROTO : ContainerProtocol.NestedClass()


object OBJECT_WITH_INTERFACE_INHERITANCE: ContainerProtocol

enum class ENUM_WITH_INTERFACE_INHERITANCE: ContainerProtocol

// FILE: existentials.kt

fun normal(value: Foeble): Foeble = value
var normal: Foeble = Bar()
fun nullable(value: Foeble?): Foeble? = value
var nullable: Foeble? = null
fun list(value: List<Foeble>): List<Foeble> = value
var list: List<Foeble> = emptyList()

// FILE: repeating_conformances.kt

package repeating_conformances

interface Foeble
interface Barable: Foeble

open class Parent1: Foeble
open class Child1: Parent1(), Foeble
open class GrandChild1: Child1(), Foeble

open class Parent2: Foeble
open class Child2: Parent2()
open class GrandChild2: Child2(), Foeble

open class Parent3: Barable
open class Child3: Parent3()
open class GrandChild3: Child3(), Foeble

open class Parent4: Foeble
open class Child4: Parent4()
open class GrandChild4: Child4(), Barable

open class Parent5
open class Child5: Parent5()
open class GrandChild5: Child5(), Barable, Foeble
