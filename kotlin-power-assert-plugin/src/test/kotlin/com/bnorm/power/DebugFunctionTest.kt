package com.bnorm.power

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.name.FqName
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals

class DebugFunctionTest {
  @Test
  fun `debug function transformation`() {
    val actual = executeMainDebug(
      """
      dbg(1 + 2 + 3)
      """.trimIndent()
    )
    assertEquals(
      """
      dbg(1 + 2 + 3)
            |   |
            |   6
            3
      """.trimIndent(),
      actual.trim()
    )
  }
  @Test
  fun `debug function transformation with message`() {
    val actual = executeMainDebug(
      """
      dbg(1 + 2 + 3, "Message:")
      """.trimIndent()
    )
    assertEquals(
      """
      Message:
      dbg(1 + 2 + 3, "Message:")
            |   |
            |   6
            3
      """.trimIndent(),
      actual.trim()
    )
  }
}

fun executeMainDebug(mainBody: String): String {
  val file = SourceFile.kotlin(
    name = "main.kt",
    contents = """
fun <T> dbg(value: T): T = value

fun <T> dbg(value: T, msg: String): T {
    println(msg)
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
