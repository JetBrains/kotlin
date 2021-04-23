package com.bnorm.power

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.name.FqName
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals

class MultiParameterFunctionTest {
  @Test
  fun `debug multi-parameter function transformation`() {
    val actual = executeMainDebug(
      """
      val operation = "sum"
      dbg(operation, 1 + 2 + 3)
      """.trimIndent()
    )
    assertEquals(
      """
      sum=6
      dbg(operation, 1 + 2 + 3)
          |            |   |
          |            |   6
          |            3
          sum
      """.trimIndent(),
      actual.trim()
    )
  }

  @Test
  fun `debug multi-parameter function transformation with complex booleans`() {
    val actual = executeMainDebug(
      """
      val greeting: String? = null
      val name: String? = null
      dbg(
        key = greeting != null && greeting.length == 5,
        value = name == null || name.length == 5
      )
      """.trimIndent()
    )
    assertEquals(
      """
      false=true
      dbg(
        key = greeting != null && greeting.length == 5,
              |        |
              |        false
              null
        value = name == null || name.length == 5
                |    |
                |    true
                null
      )
      """.trimIndent(),
      actual.trim()
    )
  }

  @Test
  fun `debug multi-parameter function transformation with message`() {
    val actual = executeMainDebug(
      """
      val operation = "sum"
      dbg(operation, 1 + 2 + 3, "Message:")
      """.trimIndent()
    )
    assertEquals(
      """
      sum=6
      Message:
      dbg(operation, 1 + 2 + 3, "Message:")
          |            |   |
          |            |   6
          |            3
          sum
      """.trimIndent(),
      actual.trim()
    )
  }

  @Test
  fun `debug multi-parameter function transformation with message and complex booleans`() {
    val actual = executeMainDebug(
      """
      val greeting: String? = null
      val name: String? = null
      dbg(
        key = greeting != null && greeting.length == 5,
        value = name == null || name.length == 5,
        msg = "Message:"
      )
      """.trimIndent()
    )
    assertEquals(
      """
      false=true
      Message:
      dbg(
        key = greeting != null && greeting.length == 5,
              |        |
              |        false
              null
        value = name == null || name.length == 5,
                |    |
                |    true
                null
        msg = "Message:"
      )
      """.trimIndent(),
      actual.trim()
    )
  }
}

private fun executeMainDebug(mainBody: String): String {
  val file = SourceFile.kotlin(
    name = "main.kt",
    contents = """
fun <T> dbg(key: Any, value: T): T = value

fun <T> dbg(key: Any, value: T, msg: String): T {
    println(key.toString() + "=" + value + "\n" + msg)
    return value
}

fun main() {
  $mainBody
}
""",
    trimIndent = false
  )

  val result = compile(listOf(file), PowerAssertComponentRegistrar(setOf(FqName("dbg"))))
  assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

  val kClazz = result.classLoader.loadClass("MainKt")
  val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
  val prevOut = System.out
  try {
    val out = ByteArrayOutputStream()
    System.setOut(PrintStream(out))
    main.invoke(null)
    return out.toString("UTF-8")
  } catch (t: InvocationTargetException) {
    throw t.cause!!
  } finally {
    System.setOut(prevOut)
  }
}
