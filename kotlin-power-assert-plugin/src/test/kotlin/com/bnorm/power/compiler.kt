/*
 * Copyright (C) 2020 Brian Norman
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
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.name.FqName
import java.lang.reflect.InvocationTargetException
import kotlin.test.assertEquals
import kotlin.test.fail

private val DEFAULT_COMPONENT_REGISTRARS = arrayOf(
  PowerAssertComponentRegistrar(setOf(FqName("kotlin.assert")))
)

fun compile(
  list: List<SourceFile>,
  vararg plugins: ComponentRegistrar = DEFAULT_COMPONENT_REGISTRARS
): KotlinCompilation.Result {
  return KotlinCompilation().apply {
    sources = list
    useIR = true
    messageOutputStream = System.out
    compilerPlugins = plugins.toList()
    inheritClassPath = true
  }.compile()
}

fun executeAssertion(
  @Language("kotlin") source: String,
  vararg plugins: ComponentRegistrar = DEFAULT_COMPONENT_REGISTRARS
): String {
  val result = compile(
    listOf(SourceFile.kotlin("main.kt", source, trimIndent = false)),
    *plugins,
  )
  assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

  val kClazz = result.classLoader.loadClass("MainKt")
  val main = kClazz.declaredMethods.single { it.name == "main" && it.parameterCount == 0 }
  try {
    try {
      main.invoke(null)
    } catch (t: InvocationTargetException) {
      throw t.cause!!
    }
    fail("should have thrown assertion")
  } catch (t: Throwable) {
    return t.message ?: ""
  }
}

fun executeMainAssertion(mainBody: String) = executeAssertion(
  """
fun main() {
  $mainBody
}
"""
)

fun assertMessage(
  @Language("kotlin") source: String,
  message: String,
  vararg plugins: ComponentRegistrar = DEFAULT_COMPONENT_REGISTRARS
) {
  val actual = executeAssertion(source, *plugins)
  assertEquals(message, actual)
}
