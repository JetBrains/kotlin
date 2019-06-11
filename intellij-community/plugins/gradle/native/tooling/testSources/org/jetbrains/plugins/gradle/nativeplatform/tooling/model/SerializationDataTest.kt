// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.nativeplatform.tooling.model

import com.intellij.serialization.ObjectSerializer
import com.intellij.serialization.ReadConfiguration
import com.intellij.serialization.WriteConfiguration
import org.jetbrains.plugins.gradle.nativeplatform.tooling.model.impl.CppProjectImpl
import org.junit.Test


class SerializationDataTest : SerializationDataTestCase() {
  @Test
  fun `test cpp project serialization`() {
    val original = generateCppProject()
    val bytes = ObjectSerializer.instance.writeAsBytes(original, WriteConfiguration(allowAnySubTypes = true))
    val deserialized = ObjectSerializer.instance.read(CppProjectImpl::class.java, bytes, ReadConfiguration(allowAnySubTypes = true))
    assertEquals(original, deserialized)
  }
}