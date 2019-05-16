// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.model

import com.intellij.openapi.externalSystem.model.internal.InternalExternalProjectInfo
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import com.intellij.serialization.ObjectSerializer
import com.intellij.serialization.VersionedFile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Before
import org.junit.Test
import java.io.Serializable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.URLClassLoader
import java.nio.file.Paths

class DataNodeTest {
  lateinit var classLoader: ClassLoader
  private val libUrl = javaClass.classLoader.getResource("dataNodeTest/lib.jar")

  @Before
  fun setUp() {
    classLoader = URLClassLoader(arrayOf(libUrl), javaClass.classLoader)
  }

  // open https://github.com/apereo/cas project in IDEA and then copy project.dat from system cache to somewhere
  //@Test
  fun testLoad() {
    var versionedFile = VersionedFile(Paths.get("/Volumes/data/big-ion.ion"), ExternalProjectsDataStorage.STORAGE_VERSION)
    var start = System.currentTimeMillis()
    val data = versionedFile.readList(InternalExternalProjectInfo::class.java, externalSystemBeanConstructed)!!
    println("Read in ${System.currentTimeMillis() - start}")

    versionedFile = VersionedFile(Paths.get("/Volumes/data/big-ion2.ion"), ExternalProjectsDataStorage.STORAGE_VERSION, isCompressed = true)

    start = System.currentTimeMillis()
    versionedFile.writeList(data, InternalExternalProjectInfo::class.java)
    println("Write in ${System.currentTimeMillis() - start}")

    start = System.currentTimeMillis()
    versionedFile.writeList(data, InternalExternalProjectInfo::class.java)
    println("Second write in ${System.currentTimeMillis() - start}")
  }

  @Test
  fun `instance of class from a classloader can be deserialized`() {
    val barObject = classLoader.loadClass("foo.Bar").newInstance()

    val deserialized = wrapAndDeserialize(barObject)

    assertThatExceptionOfType(IllegalStateException::class.java)
      .isThrownBy { deserialized.deserializeData(listOf(javaClass.classLoader)) }

    deserialized.deserializeData(listOf(URLClassLoader(arrayOf(libUrl), javaClass.classLoader)))
    assertThat(deserialized.data.javaClass.name).isEqualTo("foo.Bar")
  }

  // well, proxy cannot be serialized because on deserialize we need class
  fun `proxy instance can be deserialized`() {
    val interfaceClass = classLoader.loadClass("foo.Baz")

    val invocationHandler = InvocationHandler { _, _, _ -> 0 }

    val proxyInstance = Proxy.newProxyInstance(classLoader, arrayOf(interfaceClass), invocationHandler)
    @Suppress("UNCHECKED_CAST")
    val deserialized = wrapAndDeserialize(proxyInstance)

    assertThatExceptionOfType(IllegalStateException::class.java)
      .isThrownBy { deserialized.deserializeData(listOf(javaClass.classLoader)) }

    deserialized.deserializeData(listOf(URLClassLoader(arrayOf(libUrl), javaClass.classLoader)))
    assertThat(deserialized.data.javaClass.interfaces)
      .extracting("name")
      .contains("foo.Baz")
  }

  @Test
  fun `ProjectSystemIds are re-used after deserialization`() {
    val id = ProjectSystemId("MyTest")

    val dataNodes = listOf(DataNode(Key.create(ProjectSystemId::class.java, 0), id, null),
                           DataNode(Key.create(ProjectSystemId::class.java, 0), id, null))

    val buffer = WriteAndCompressSession()
    dataNodes.forEach { it.serializeData(buffer) }
    val out = BufferExposingByteArrayOutputStream()
    ObjectSerializer.instance.writeList(dataNodes, DataNode::class.java, out)
    val bytes = out.toByteArray()
    val deserializedList = ObjectSerializer.instance.readList(DataNode::class.java, bytes, createDataNodeReadConfiguration(javaClass.classLoader))

    assertThat(deserializedList).hasSize(2)
    assertThat(deserializedList[0].data === deserializedList[1].data)
      .`as`("project system ID instances should be re-used")
      .isTrue()
  }

  fun `proxy instance referenced from invocation handler (de-)serialized`() {
    val handler = object: InvocationHandler, Serializable {
      var counter: Int = 0
      override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        return when (method?.name) {
          "incrementAndGet" -> ++counter
          else -> Unit
        }
      }

      var ref: Counter? = null
    }

    val proxy = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Counter::class.java, Serializable::class.java), handler) as Counter
    handler.ref = proxy
    assertThat(proxy.incrementAndGet()).isEqualTo(1)

    val dataNode = wrapAndDeserialize(proxy)
    val counter = dataNode.data as Counter
    assertThat(counter.incrementAndGet()).isEqualTo(2)
  }

  private fun wrapAndDeserialize(barObject: Any): DataNode<*> {
    val original = DataNode(Key.create(barObject.javaClass, 0), barObject, null)
    original.serializeData(WriteAndCompressSession())
    val bytes = ObjectSerializer.instance.writeAsBytes(original)
    return ObjectSerializer.instance.read(DataNode::class.java, bytes)
  }
}

private interface Counter {
  fun incrementAndGet(): Int
}


