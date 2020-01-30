// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

class GroovyBuilder(private val indent: String = "") {

  private val builder = StringBuilder()

  fun line(text: String) = apply {
    builder.appendln(indent + text)
  }

  fun call(name: String, value: Any?) = apply {
    line("$name $value")
  }

  fun property(name: String, value: Any?) = apply {
    line("$name = $value")
  }

  fun propertyIfNotNull(name: String, value: Any?) = apply {
    if (value != null) {
      property(name, value)
    }
  }

  fun generate() = builder.toString()

  companion object {
    fun generate(indent: String = "", configure: GroovyBuilder.() -> Unit): String {
      val builder = GroovyBuilder(indent)
      builder.configure()
      return builder.generate()
    }
  }
}