typealias TInt = Int
typealias TUint = UInt

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

    val aliasedInt: TInt = 1
    val aliasedUInt: TUint = 1U

    val b: Byte = 1
    val s: Short = 1
    val x: Float = 1f

    val ubyte1: UByte = 1u
    val ushort1: UShort = 1u
    val uint1: UInt = 1u
    val ulong1: ULong = 1u

    val ubyteMax: UByte = UByte.MAX_VALUE
    val ushortMax: UShort = UShort.MAX_VALUE
    val uintMax: UInt = UInt.MAX_VALUE
    val ulongMax: ULong = ULong.MAX_VALUE
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
    val floatArray = floatArrayOf(-1.0f)
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
