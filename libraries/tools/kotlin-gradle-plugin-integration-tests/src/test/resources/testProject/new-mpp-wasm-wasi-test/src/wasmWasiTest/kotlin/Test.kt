package my.pack.name

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class WasmTest {
    @Test
    fun test1() = assertEquals(foo(), 1)

    @Test
    @Ignore
    fun test2() = assertEquals(foo(), 2)

    @Test
    fun test3() = assertEquals(foo(), 3)

    @Ignore
    class InnerIgnored {
        @Test
        fun test4() = assertEquals(foo(), 1)

        @Test
        @Ignore
        fun test5() = assertEquals(foo(), 2)

        @Test
        fun test6() = assertEquals(foo(), 3)
    }
}