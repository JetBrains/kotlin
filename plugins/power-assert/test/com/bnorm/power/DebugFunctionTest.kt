/*
 * Copyright (C) 2021 Brian Norman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bnorm.power

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.name.FqName
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class DebugFunctionTest {
  @Test
  fun `debug function transformation`() {
    val actual = executeMainDebug(
      """
      dbg(1 + 2 + 3)
      """.trimIndent(),
    )
    assertEquals(
      """
      dbg(1 + 2 + 3)
            |   |
            |   6
            3
      """.trimIndent(),
      actual.trim(),
    )
  }

  @Test
  fun `debug function transformation with message`() {
    val actual = executeMainDebug(
      """
      dbg(1 + 2 + 3, "Message:")
      """.trimIndent(),
    )
    assertEquals(
      """
      Message:
      dbg(1 + 2 + 3, "Message:")
            |   |
            |   6
            3
      """.trimIndent(),
      actual.trim(),
    )
  }
}

private fun executeMainDebug(mainBody: String): String {
  val file = SourceFile.kotlin(
    name = "main.kt",
    contents = """
fun <T> dbg(value: T): T = value

fun <T> dbg(value: T, msg: String): T {
    throw RuntimeException("result:"+msg)
    return value
}

fun main() {
  $mainBody
}
""",
    trimIndent = false,
  )

  val result = compile(listOf(file), PowerAssertCompilerPluginRegistrar(setOf(FqName("dbg"))))
  assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

  val kClazz = result.classLoader.loadClass("MainKt")
  val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
  return getMainResult(main)
}

fun getMainResult(main: Method): String {
  try {
    main.invoke(null)
    fail("main did not throw expected exception")
  } catch (t: InvocationTargetException) {
    with(t.cause) {
      if (this is RuntimeException && message != null && message!!.startsWith("result:")) {
        return message!!.substringAfter("result:")
      }
    }
    throw t.cause!!
  }
}
