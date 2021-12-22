// DONT_TARGET_EXACT_BACKEND: JS
// KJS_WITH_FULL_RUNTIME

// Generated briges must not contain any casts!
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=collect1
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=collect2
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=collect3
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=collect4
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=collect4
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumBools
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumBytes
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumShorts
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumChars
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumInts
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumFloats
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumDoubles
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumLongs
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumUByte
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumUShort
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumUInt
// CHECK_NOT_CALLED_IN_SCOPE: function=THROW_CCE scope=sumULong

external interface Collector {

    // Both leading and trailing parameters are present
    fun collect1(leading1: String, leading2: String, vararg xs: String, trailing1: String, trailing2: String, trailing3: String): String

    // No trailing parameters
    fun collect2(leading: String, vararg xs: String): String

    // No leading parameters
    fun collect3(vararg xs: String, trailing: String): String

    // Vararg only
    fun collect4(vararg xs: String): String
}

class CollectorImpl : Collector {
    override fun collect1(
        leading1: String,
        leading2: String,
        vararg xs: String,
        trailing1: String,
        trailing2: String,
        trailing3: String
    ): String {
        var result = "[$leading1, $leading2, ["
        for (i in 0 until xs.size) {
            if (i > 0) result += ", "
            result += xs[i]
        }
        result += "], $trailing1, $trailing2, $trailing3]"
        return result
    }

    override fun collect2(leading: String, vararg xs: String) =
        collect1("∅", leading, *xs, trailing1 = "∅", trailing2 = "∅", trailing3 = "∅")

    override fun collect3(vararg xs: String, trailing: String) =
        collect1("∅", "∅", *xs, trailing1 = trailing, trailing2 = "∅", trailing3 = "∅")

    override fun collect4(vararg xs: String) = collect1("∅", "∅", *xs, trailing1 = "∅", trailing2 = "∅", trailing3 = "∅")
}

fun collectNames1(collector: Collector, names: Array<String>) =
    collector.collect1("Jack", "Kate", *names, trailing2 = "Winsent", trailing1 = "Claire", trailing3 = "Sawyer")

fun collectNames2(collector: Collector, names: Array<String>) =
    collector.collect2("Jack", *names)

fun collectNames3(collector: Collector, names: Array<String>) =
    collector.collect3(*names, trailing = "Winsent")

fun collectNames4(collector: Collector, names: Array<String>) =
    collector.collect4(*names)

external interface Folder {
    fun sumBools(vararg xs: Boolean): Boolean
    fun sumBytes(vararg xs: Byte): Int
    fun sumShorts(vararg xs: Short): Int
    fun sumChars(vararg xs: Char): String
    fun sumInts(vararg xs: Int): Int
    fun sumFloats(vararg xs: Float): Float
    fun sumDoubles(vararg xs: Double): Double
    fun sumLongs(vararg xs: Long): Long

    fun sumUByte(vararg xs: UByte): UInt
    fun sumUShort(vararg xs: UShort): UInt
    fun sumUInt(vararg xs: UInt): UInt
    fun sumULong(vararg xs: ULong): ULong
}

class FolderImpl: Folder {

    // Test with primitive types. Varargs of primitive types expect primitive arrays.
    override fun sumBools(vararg xs: Boolean) = xs.fold(true, Boolean::or)
    override fun sumBytes(vararg xs: Byte) = xs.fold(0, Int::plus)
    override fun sumShorts(vararg xs: Short) = xs.fold(0, Int::plus)
    override fun sumChars(vararg xs: Char) = xs.fold("", String::plus)
    override fun sumInts(vararg xs: Int) = xs.fold(0, Int::plus)
    override fun sumFloats(vararg xs: Float) = xs.fold(0.0f, Float::plus)
    override fun sumDoubles(vararg xs: Double) = xs.fold(0.0, Double::plus)
    override fun sumLongs(vararg xs: Long) = xs.fold(0L, Long::plus)

    // Test with inline classes
    override fun sumUByte(vararg xs: UByte) = xs.fold(0U, UInt::plus)
    override fun sumUShort(vararg xs: UShort) = xs.fold(0U, UInt::plus)
    override fun sumUInt(vararg xs: UInt) = xs.fold(0U, UInt::plus)
    override fun sumULong(vararg xs: ULong) = xs.fold(0UL, ULong::plus)
}

fun box(): String {
    val collector: Collector = CollectorImpl()

    assertEquals("[1, 2, [3, 4], 5, 6, 7]", collector.collect1("1", "2", "3", "4", trailing2 = "6", trailing1 = "5", trailing3 = "7"))
    assertEquals("[∅, 1, [2, 3, 4, 5, 6, 7], ∅, ∅, ∅]", collector.collect2("1", "2", "3", "4", "5", "6", "7"))
    assertEquals("[∅, ∅, [1, 2, 3], 4, ∅, ∅]", collector.collect3("1", "2", "3", trailing = "4"))
    assertEquals("[∅, ∅, [], ∅, ∅, ∅]", collector.collect4())

    assertEquals("[Jack, Kate, [Hugo, Jin], Claire, Winsent, Sawyer]", collectNames1(collector, arrayOf("Hugo", "Jin")))
    assertEquals("[∅, Jack, [Kate, Hugo, Jin], ∅, ∅, ∅]", collectNames2(collector, arrayOf("Kate", "Hugo", "Jin")))
    assertEquals("[∅, ∅, [Claire], Winsent, ∅, ∅]", collectNames3(collector, arrayOf("Claire")))
    assertEquals("[∅, ∅, [], ∅, ∅, ∅]", collectNames4(collector, arrayOf()))

    assertEquals(
        "[4, 8, [15], 16, 23, 42]",
        js("collector.collect1('4', '8', '15', '16', '23', '42')").unsafeCast<String>()
    )
    assertEquals(
        "[∅, 4, [8, 15, 16, 23, 42], ∅, ∅, ∅]",
        js("collector.collect2('4', '8', '15', '16', '23', '42')").unsafeCast<String>()
    )
    assertEquals(
        "[∅, ∅, [4, 8, 15, 16, 23], 42, ∅, ∅]",
        js("collector.collect3('4', '8', '15', '16', '23', '42')").unsafeCast<String>()
    )
    assertEquals(
        "[∅, ∅, [], ∅, ∅, ∅]",
        js("collector.collect4()").unsafeCast<String>()
    )

    val folder: Folder = FolderImpl()

    assertEquals(true, folder.sumBools(false, true, false, false))
    assertEquals(10, folder.sumBytes(1, 2, 3, 4))
    assertEquals(10, folder.sumShorts(1, 2, 3, 4))
    assertEquals("Hello", folder.sumChars('H', 'e', 'l', 'l', 'o'))
    assertEquals(10, folder.sumInts(1, 2, 3, 4))
    assertEquals(10.0f, folder.sumFloats(1.0f, 2.0f, 3.0f, 4.0f))
    assertEquals(10.0, folder.sumDoubles(1.0, 2.0, 3.0, 4.0))
    assertEquals(10L, folder.sumLongs(1L, 2L, 3L, 4L))

    assertEquals(10U, folder.sumUByte(1U, 2U, 3U, 4U))
    assertEquals(10U, folder.sumUShort(1U, 2U, 3U, 4U))
    assertEquals(10U, folder.sumUInt(1U, 2U, 3U, 4U))
    assertEquals(10U, folder.sumULong(1U, 2U, 3U, 4U))

    return "OK"
}
