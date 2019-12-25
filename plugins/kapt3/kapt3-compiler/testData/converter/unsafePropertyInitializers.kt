object Foo {
    const val aString: String = "foo"
    const val aInt: Int = 3

    val bString: String = "bar"
    val bInt: Int = 5

    var cString: String = "baz"
    var cInt: Int = 7

    val d = Boo.z
    val e = Boo.z.length
    val f = 5 + 3
    val g = "a" + "b"
    val h = -4
    val i = Int.MAX_VALUE
    val j = "$e$g"
    val k = g + j
}

object Boo {
    val z = foo()
    fun foo() = "abc"
}

class HavingState {
    val state = State.START
    val stateArray = arrayOf(State.START)
    val stringArray = arrayOf("foo")
    val stringList = listOf("foo")
    val intArray = arrayOf(1)
    val intList = listOf(1)
    val uint = 1U
    val uintArray = arrayOf(1U)
    val uintList = listOf(1U)
    val clazz = State::class
    val javaClass = State::class.java
    val anonymous = (object {})::class
}

enum class State {
    START,
    FINISH,
}