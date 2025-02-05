import org.junit.jupiter.api.MethodOrderer.MethodName
import org.junit.jupiter.api.TestMethodOrder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// const val remoteDebug = true
const val remoteDebug = false

val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { it.resolve("ReadMe.md").exists() }
const val debugPort = 5005

@TestMethodOrder(MethodName::class)
class Test {
    @Test
    fun basicOverloadResolution() = doTest {
        mainKt = """
            fun <T> id(t: T) = t
            @JvmName("outer1") fun outer(a: List<Int>): Unit = error("fail")
            @JvmName("outer2") fun outer(a: Set<String>) = Unit
            fun main() {
                outer([""])
                outer([id("")])
            }
        """
    }

    @Test
    fun passCollectionLiteralToGeneric_green() = doTest { // todo fails
        mainKt = """
            fun <T> outer(t: T) = Unit
            fun main() = outer<Set<Int>>([1])
        """.trimIndent()
    }

    @Test
    fun passCollectionLiteralToGeneric_red() = doTest { // todo better error message
        mainKt = """
            fun <T> outer(t: T) = Unit
            fun main() = outer([1])
        """.trimIndent()
        expectedCompilationError = """
            main.kt:2:14: error: [CANNOT_INFER_PARAMETER_TYPE] Cannot infer type for this parameter. Specify it explicitly.
            fun main() = outer([1])
                         ^^^^^
            main.kt:2:20: error: [EXPECTED_TYPE_DOESNT_CONTAIN_COMPANION_OPERATOR_OF_FUNCTION] Type 'TypeVariable(T)' doesnt contain 'operator fun of' function in its 'companion object'
            fun main() = outer([1])
                               ^^^
        """.trimIndent()
    }

    @Test
    fun passCollectionLiteralToGenericWithUpperBound_red() = doTest {
        mainKt = """
            fun <T : List<Int>> outer(t: T) = Unit
            fun main() {
                outer([1])
            }
        """.trimIndent()
        expectedCompilationError = """
            main.kt:3:5: error: [CANNOT_INFER_PARAMETER_TYPE] Cannot infer type for this parameter. Specify it explicitly.
                outer([1])
                ^^^^^
            main.kt:3:11: error: [EXPECTED_TYPE_DOESNT_CONTAIN_COMPANION_OPERATOR_OF_FUNCTION] Type 'TypeVariable(T)' doesnt contain 'operator fun of' function in its 'companion object'
                outer([1])
                      ^^^
        """.trimIndent()
    }

    @Test
    fun reassignment() = doTest {
        mainKt = """
            class IntList { companion object { fun of(vararg elems: Int): IntList = IntList() } }
            fun main() {
                var x: IntList = [1]
                x = [1]
                @Suppress("USELESS_IS_CHECK")
                check(x is IntList)
            }
        """.trimIndent()
    }

    @Test
    fun basicOverloadResolution_red() = doTest {
        mainKt = """
            fun <T> id(t: T) = t
            @JvmName("outer1") fun outer(a: List<Int>): Unit = error("fail")
            @JvmName("outer2") fun outer(a: List<String>) = Unit
            fun main() = outer(id([""]))
        """
        expectedCompilationError = """
            main.kt:5:32: error: [CANNOT_INFER_PARAMETER_TYPE] Cannot infer type for this parameter. Specify it explicitly.
                        fun main() = outer(id([""]))
                                           ^^
            main.kt:5:35: error: [EXPECTED_TYPE_DOESNT_CONTAIN_COMPANION_OPERATOR_OF_FUNCTION] Type 'TypeVariable(T)' doesnt contain 'operator fun of' function in its 'companion object'
                        fun main() = outer(id([""]))
                                              ^^^^
        """.trimIndent()
    }

    @Test
    fun sharedTypeVariableBecomesLambda() = doTest {
        mainKt = """
            fun <T> outer(a: List<T>, t: T) = Unit
            fun main() = outer([{ it.length }], { a: String -> })
        """
    }

    @Test
    fun sharedTypeVariableBecomesLambda_red() = doTest {
        mainKt = """
            fun <T> outer(a: List<T>, t: T) = Unit
            fun main() = outer([{ it.length }], { a: Int -> })
        """.trimIndent()
        expectedCompilationError = """
            main.kt:2:26: error: [UNRESOLVED_REFERENCE] Unresolved reference 'length'.
            fun main() = outer([{ it.length }], { a: Int -> })
                                     ^^^^^^
        """.trimIndent()
    }

    @Test
    fun requireOperatorKeyword() = doTest {
        mainKt = """
            class IntList { companion object { fun of(vararg elems: Int): IntList = IntList() } }
            fun main() {
                val x: IntList = [1]
            }
        """.trimIndent()
        expectedCompilationError = """
            missing operator keyword
        """.trimIndent()
    }

    @Test
    fun customIntList_ofOverloads() = doTest {
        mainKt = """
            class IntList(val x: String) { companion object { 
                fun of(elem: Int): IntList = IntList("1")
                fun of(vararg elems: Int): IntList = IntList("2")
            } }
            fun main() {
                val x: IntList = [1]
                val y: IntList = [1, 2]
                print(x.x + y.x)
            }
        """.trimIndent()
        expectedRuntimeOutput = "12"
    }

    @Test
    fun customIntList_green() = doTest {
        mainKt = """
            class IntList { companion object { fun of(vararg elems: Int): IntList = IntList() } }
            fun main() {
                val intList: IntList = [1]
            }
            fun foo(): IntList = [1]
        """.trimIndent()
    }

    @Test
    fun customIntList_red() = doTest {
        mainKt = """
            class IntList { companion object { fun of(vararg elems: Int): IntList = IntList() } }
            fun main() {
                val intList: IntList = [""]
            }
        """.trimIndent()
        expectedCompilationError = """
            main.kt:3:29: error: [ARGUMENT_TYPE_MISMATCH] Argument type mismatch: actual type is 'kotlin.String', but 'kotlin.Int' was expected.
                val intList: IntList = [""]
                                        ^^
        """.trimIndent()
    }

    @Test
    fun expectedTypeFromFunParameter() = doTest {
        mainKt = """
            fun outer(a: List<String>) = Unit
            fun main() = outer([""])
        """
    }

    @Test
    fun sandbox() = doTest {
        mainKt = """
            fun outer(a: List<Int>) = Unit
            fun main() {
                outer([1])
            }
        """.trimIndent()
    }

    @Test
    fun basics() = doTest {
        mainKt = """
            fun println(a: Any) = Unit
            fun acceptList(a: List<Int>) = Unit
            fun main() {
                val x = [1]
                val y = ["foo"]
                val z: Any = [1]
                println(x)
                acceptList(x)
                println([1])
                [1]

                println(y)
                println([""])
            }
            fun foo(): List<Int> = [1]
        """
    }

    @Test
    fun explicitTypes() = doTest {
        mainKt = """
            fun main() {
                val x: List<Int> = [1]
                val y: List<String> = ["foo"]
            }
        """
    }

    @Test
    fun forwardInnerMaterializeTypeVariable() = doTest { // covers change in processConstraintStorageFromAtom
        mainKt = """
            @Suppress("UNCHECKED_CAST") fun <K> materialize(): K = 1 as K
            fun outerCall(a: IntList) = Unit
            fun outerCall(a: DoubleList) = Unit

            fun main() {
                outerCall([1, 2, materialize()])
            }

            class IntList { companion object {
                operator fun of(vararg x: Int): IntList = IntList()
            } }

            class DoubleList { companion object {
                operator fun of(vararg x: Double): DoubleList = DoubleList()
            } }
        """.trimIndent()
    }

    @Test
    fun matrix() = doTest {
        mainKt = """
            fun main() {
                val x = [[1]]
                val y: List<List<Int>> = [[1]]
            }
        """
    }

    @Test
    fun innerIntVsLongOverload() = doTest { // todo reconsider as separate language feature
        mainKt = """
            @JvmName("outer1") fun outer(a: Iterable<Long>): Unit = error("fail")
            @JvmName("outer2") fun outer(a: Iterable<Int>): Unit = Unit
            fun main() {
                outer([1])
                outer(listOf(2))
            }
        """.trimIndent()
        expectedCompilationError = """
            main.kt:4:5: error: [OVERLOAD_RESOLUTION_AMBIGUITY] Overload resolution ambiguity between candidates:
            fun outer(a: Iterable<Long>): Unit
            fun outer(a: Iterable<Int>): Unit
                outer([1])
                ^^^^^
            main.kt:5:5: error: [OVERLOAD_RESOLUTION_AMBIGUITY] Overload resolution ambiguity between candidates:
            fun outer(a: Iterable<Long>): Unit
            fun outer(a: Iterable<Int>): Unit
                outer(listOf(2))
                ^^^^^
        """.trimIndent()
    }

    @Test
    fun iterableOverloads() = doTest {
        mainKt = """
            @JvmName("outer1") fun outer(a: Iterable<String>): Unit = error("fail")
            @JvmName("outer2") fun outer(a: Iterable<Int>): Unit = Unit
            fun main() = outer([1])
        """.trimIndent()
    }

    @Test
    fun nestedCallableReference() = doTest {
        mainKt = """
            fun main() {
                println(outer([::foo]))
            }

            @JvmName("outer1") fun outer(a: List<(Int) -> String>): String = "1" + a.first()(1)
            @JvmName("outer2") fun outer(b: List<(Long) -> String>): String = "2" + b.first()(1L)

            fun foo(a: String) = "string"
            fun foo(a: Int) = "int"
        """

        expectedRuntimeOutput = "1int"
    }
}

class TestBuilder {
    var mainKt: String? = null
    var expectedCompilationError: String = ""
    var expectedRuntimeOutput: String = ""
}

fun doTest(setup: TestBuilder.() -> Unit) {
    val test = TestBuilder()
    test.setup()

    if (remoteDebug) {
        "/Users/Nikita.Bobko/.bin/kill-process-on-port $debugPort".binBashExec(File("").absoluteFile)
        println("Waiting for remote debug...")
    }

    val tmpDir = File("/tmp/kotlin-cl-tests/")
    try {
        tmpDir.deleteRecursively()
        check(tmpDir.mkdirs())

        tmpDir.resolve("main.kt").writeText(test.mainKt ?: error("mainKt is mandatory"))

        val (actualCompilationOutput, compilationExitCode) = """
            $root/dist/kotlinc/bin/kotlinc \
                -Xrender-internal-diagnostic-names \
                main.kt 2> /dev/stdout
        """.trimIndent().binBashExec(tmpDir)
        assertEquals(test.expectedCompilationError.trim(), actualCompilationOutput.trim())
        if (test.expectedCompilationError.isEmpty()) {
            assertEquals(0, compilationExitCode)
        } else {
            assertNotEquals(0, compilationExitCode)
            return
        }

        val (actualRuntimeOutput, runtimeExitCode) = """
            $root/dist/kotlinc/bin/kotlin MainKt 2> /dev/stdout
        """.trimIndent().binBashExec(tmpDir)
        assertEquals(test.expectedRuntimeOutput.trim(), actualRuntimeOutput.trim())
        assertEquals(0, runtimeExitCode)
    } finally {
        tmpDir.deleteRecursively()
    }
}

fun String.binBashExec(dir: File): Pair<String, Int> {
    val builder = ProcessBuilder(
        "/bin/bash",
        "-c",
        """
            set -e
            $this
        """.trimIndent()
    )
        .directory(dir)
        .apply {
            if (remoteDebug) {
                environment().put("JAVA_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:$debugPort")
            }
        }
    val process = builder.start()
    val actualOutput = process.inputStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    return Pair(actualOutput, exitCode)
}
