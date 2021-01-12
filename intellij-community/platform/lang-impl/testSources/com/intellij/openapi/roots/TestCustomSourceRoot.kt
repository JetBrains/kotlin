// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("unused")

package com.intellij.openapi.roots

import com.intellij.jps.impl.JpsPluginBean
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.extensions.DefaultPluginDescriptor
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.lang.UrlClassLoader
import org.jdom.Element
import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.ex.JpsElementTypeBase
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension
import org.jetbrains.jps.model.serialization.module.JpsModuleSourceRootPropertiesSerializer
import java.io.File

class TestCustomRootModelSerializerExtension : JpsModelSerializerExtension() {
  override fun getModuleSourceRootPropertiesSerializers(): List<JpsModuleSourceRootPropertiesSerializer<*>> =
    listOf(TestCustomSourceRootPropertiesSerializer(TestCustomSourceRootType.INSTANCE, TestCustomSourceRootType.TYPE_ID))

  companion object {
    fun registerTestCustomSourceRootType(tempPluginRoot: File, disposable: Disposable) {
      val jpsPluginDisposable = Disposer.newDisposable()
      Disposer.register(disposable, Disposable {
        runWriteActionAndWait {
          Disposer.dispose(jpsPluginDisposable)
        }
      })

      FileUtil.writeToFile(File(tempPluginRoot, "META-INF/services/${JpsModelSerializerExtension::class.java.name}"),
                           TestCustomRootModelSerializerExtension::class.java.name)
      val pluginClassLoader = UrlClassLoader.build().parent(TestCustomRootModelSerializerExtension::class.java.classLoader).urls(tempPluginRoot.toURI().toURL()).get()
      val pluginDescriptor = DefaultPluginDescriptor(PluginId.getId("com.intellij.custom.source.root.test"), pluginClassLoader)
      JpsPluginBean.EP_NAME.point.registerExtension(JpsPluginBean(), pluginDescriptor, jpsPluginDisposable)
    }

  }
}

class TestCustomSourceRootType private constructor() : JpsElementTypeBase<TestCustomSourceRootProperties>(), JpsModuleSourceRootType<TestCustomSourceRootProperties> {
  override fun isForTests(): Boolean = false

  override fun createDefaultProperties(): TestCustomSourceRootProperties = TestCustomSourceRootProperties("default properties")

  companion object {
    val INSTANCE = TestCustomSourceRootType()
    const val TYPE_ID = "custom-source-root-type"
  }
}

class TestCustomSourceRootProperties(initialTestString: String?) : JpsElementBase<TestCustomSourceRootProperties>() {
  var testString: String? = initialTestString
    set(value) {
      if (value != field) {
        field = value
        fireElementChanged()
      }
    }

  override fun createCopy(): TestCustomSourceRootProperties {
    return TestCustomSourceRootProperties(testString)
  }

  override fun applyChanges(modified: TestCustomSourceRootProperties) {
    testString = modified.testString
  }
}

class TestCustomSourceRootPropertiesSerializer(
  type: JpsModuleSourceRootType<TestCustomSourceRootProperties>, typeId: String)
  : JpsModuleSourceRootPropertiesSerializer<TestCustomSourceRootProperties>(type, typeId) {

  override fun loadProperties(sourceRootTag: Element): TestCustomSourceRootProperties {
    val testString = sourceRootTag.getAttributeValue("testString")
    return TestCustomSourceRootProperties(testString)
  }

  override fun saveProperties(properties: TestCustomSourceRootProperties, sourceRootTag: Element) {
    val testString = properties.testString

    if (testString != null) {
      sourceRootTag.setAttribute("testString", testString)
    }
  }
}
