import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testVarargs() {
    assertEquals(
            "a b -1",
            TestVarargs.testVarargsWithFormat(
                    "%@ %s %d",
                    "a" as NSString, "b".cstr, (-1).toByte()
            ).formatted
    )

    assertEquals(
            "2 3 9223372036854775807",
            TestVarargs(
                    "%d %d %lld",
                    2.toShort(), 3, Long.MAX_VALUE
            ).formatted
    )

    assertEquals(
            "0.1 0.2 1 0",
            TestVarargs.create(
                    "%.1f %.1lf %d %d",
                    0.1.toFloat(), 0.2, true, false
            ).formatted
    )

    assertEquals(
            "1 2 3",
            TestVarargs(
                    format = "%d %d %d",
                    args = *arrayOf(1, 2, 3)
            ).formatted
    )

    assertEquals(
            "4 5 6",
            TestVarargs(
                    args = *arrayOf(4, *arrayOf(5, 6)),
                    format = "%d %d %d"
            ).formatted
    )

    assertEquals(
            "7",
            TestVarargsSubclass.stringWithFormat(
                    "%d",
                    7
            )
    )
}