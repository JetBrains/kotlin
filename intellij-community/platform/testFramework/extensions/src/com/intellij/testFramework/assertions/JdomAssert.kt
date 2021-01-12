// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework.assertions

import com.intellij.configurationStore.deserialize
import com.intellij.configurationStore.serialize
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.rt.execution.junit.FileComparisonFailure
import com.intellij.util.io.readText
import com.intellij.util.isEmpty
import org.assertj.core.api.AbstractAssert
import org.assertj.core.internal.Objects
import org.intellij.lang.annotations.Language
import org.jdom.Element
import java.io.File
import java.nio.file.Path

class JdomAssert(actual: Element?) : AbstractAssert<JdomAssert, Element?>(actual, JdomAssert::class.java) {
  fun isEmpty(): JdomAssert {
    isNotNull

    if (!actual.isEmpty()) {
      failWithMessage("Expected to be empty but was\n${JDOMUtil.writeElement(actual!!)}")
    }

    return this
  }

  @Deprecated("isEqualTo(file: Path)", ReplaceWith("isEqualTo(file.toPath())"))
  fun isEqualTo(file: File) = isEqualTo(file.toPath())

  fun isEqualTo(file: Path): JdomAssert {
    isNotNull

    val expected = JDOMUtil.load(file)
    if (!JDOMUtil.areElementsEqual(actual, expected)) {
      throw FileComparisonFailure(null, StringUtilRt.convertLineSeparators(file.readText()), JDOMUtil.writeElement(actual!!), file.toString())
    }
    return this
  }

  fun isEqualTo(element: Element?): JdomAssert {
    if (actual == element) {
      return this
    }

    isNotNull

    if (!JDOMUtil.areElementsEqual(actual, element)) {
      isEqualTo(JDOMUtil.writeElement(element!!))
    }
    return this
  }

  fun isEqualTo(@Language("xml") expected: String): JdomAssert {
    isNotNull

    Objects.instance().assertEqual(
      info,
      JDOMUtil.writeElement(actual!!),
      expected.trimIndent().removePrefix("""<?xml version="1.0" encoding="UTF-8"?>""").trimStart())

    return this
  }
}

fun <T : Any> doSerializerTest(@Language("XML") expectedText: String, bean: T): T {
  // test deserializer
  val expectedTrimmed = expectedText.trimIndent()
  val element = assertSerializer(bean, expectedTrimmed)

  // test deserializer
  val o = (element ?: Element("state")).deserialize(bean.javaClass)
  assertSerializer(o, expectedTrimmed, "Deserialization failure")
  return o
}

private fun assertSerializer(bean: Any, expected: String, description: String = "Serialization failure"): Element? {
  val element = serialize(bean)
  Assertions.assertThat(element?.let { JDOMUtil.writeElement(element).trim() }).`as`(description).isEqualTo(expected)
  return element
}