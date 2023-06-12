// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlin-atomicfu-compiler-plugin-1.9.255-SNAPSHOT-atomicfu-1.jar

import kotlinx.atomicfu.*
import kotlin.test.*

class ParameterizedInlineFunExtensionTest {

    private inline fun <S> AtomicRef<S>.foo(res1: S, res2: S, foo: (S) -> S): S {
        val res = bar(res1, res2)
        return res
    }

    private inline fun <S> AtomicRef<S>.bar(res1: S, res2: S): S {
        return res2
    }

    private val tail = atomic("aaa")

    fun testClose() {
        val res = tail.foo("bbb", "ccc") { s -> s }
        assertEquals("ccc", res)
    }
}

@Test
fun testParameterizedInlineFunExtensionTest() {
    val testClass = ParameterizedInlineFunExtensionTest()
    testClass.testClose()
}
