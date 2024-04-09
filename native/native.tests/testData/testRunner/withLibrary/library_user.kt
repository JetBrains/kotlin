@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.internal.test.*
import kotlin.test.*
import library.*

open class B : A() {
    // Override test methods without a test annotation.
    // We should run these methods anyway.

    override fun before() {
        output.add("B.before")
    }

    // test0 should be executed.

    // Should be executed.
    override fun test1() {
        output.add("B.test1")
    }

    // Should be executed
    @Ignore
    override fun test2() {
        output.add("B.test2")
    }

    // Should be ignored.
    @Ignore
    @Test
    override fun test3() {
        output.add("B.test3")
    }

    // ignored0 should be ignored.

    // Should be ignored.
    override fun ignored1() {
        output.add("B.ignored1")
    }

    // Should be executed.
    @Test
    override fun ignored2() {
        output.add("B.ignored2")
    }
}

// All test methods from B should be executed for C.
class C: B() {}

class D: I1, I2 {
    // This method shouldn't be executed because its parent methods annotated
    // with @Test belong to an interface instead of an abstract class.
    override fun foo(){
        output.add("D.foo")
    }
}

fun main(args: Array<String>) {
    testLauncherEntryPoint(args)
    output.forEach(::println)
    assertEquals(8, output.count { it == "A.after" })
    assertEquals(8, output.count { it == "B.before" })

    assertEquals(2, output.count { it == "A.test0" })
    assertEquals(2, output.count { it == "B.test1" })
    assertEquals(2, output.count { it == "B.test2" })
    assertEquals(2, output.count { it == "B.ignored2" })

    assertTrue(output.none { it == "B.test3" })
    assertTrue(output.none { it == "A.ignored0" })
    assertTrue(output.none { it == "B.ignored1" })

    assertTrue(output.none { it == "D.foo" })
}