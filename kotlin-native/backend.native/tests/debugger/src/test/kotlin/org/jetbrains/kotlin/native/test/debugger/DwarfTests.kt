package org.jetbrains.kotlin.native.test.debugger

import junit.framework.Assert.*
import org.junit.Ignore
import org.junit.Test

class DwarfTests {
    @Test
    fun `prefix test`() = dwarfDumpTest("""
        fun main(args: Array<String>) {
            val xs = intArrayOf(3, 5, 8)
            return
        }

        data class Point(val x: Int, val y: Int)
    """.trimIndent(), listOf("-Xdebug-prefix-map=${System.getProperty("user.home")}=/xxx")){
        val map = flatMap( fun (it: DwarfTag): List<DwarfAttribute> { return it.attributes.values.toList()})
                .filter { it.attribute == DwarfAttribute.Attribute.DW_AT_decl_file }.map { it.rvString }
        assertNotSame(0, map.size)
    }

    /**
     * TODO: to enable this test it's required to fix issue with poisonig call site with wrong file owner ship of lambda
     * passed as parameter to inline function.
     */
    @Test
    fun `address of VolatileLambda lookup`() = dwarfDumpComplexTest {
        val callbackLibrary = """
            ---
            int callback(int (*f)(int)) {
              return 42 + f(0xdeadbeef);
            }
        """.trimIndent().cinterop("callback", "callback")
        val trapLibrary = """
            ---
            void trap() {
              __builtin_trap();
            }
        """.trimIndent().cinterop("trap", "trap")

        val poisonLibrary = """
            package poison
            import trap.*
            import callback.*
            import kotlinx.cinterop.staticCFunction


            inline fun execute():Int {
              return callback(staticCFunction{
                a ->
                   trap()
                   2 * a
              })
            }
        """.trimIndent().library("poison", "-l", callbackLibrary.toString(), "-l", trapLibrary.toString())

        val binary = """
            import poison.*

            fun main() {
              execute()
            }
        """.trimIndent().binary("poisoned", "-g", "-l", poisonLibrary.toString(), "-l", callbackLibrary.toString(), "-l", trapLibrary.toString())

        binary.dwarfDumpLookup("kfun:main\$<anonymous>_1#internal") {
            assertFalse(this.isEmpty())
            val subprogram = single { it.tag == DwarfTag.Tag.DW_TAG_subprogram } as DwarfTagSubprogram
            assertEquals(subprogram.file!!.name, "poison.kt")
        }
    }


}