/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.openapi.externalSystem.model


import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Before
import org.junit.Test
import java.io.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.URL
import java.net.URLClassLoader


class DataNodeTest {

  lateinit var cl: ClassLoader
  private val myLibUrl: URL = javaClass.classLoader.getResource("dataNodeTest/lib.jar")

  @Before
  fun setUp() {
    cl = URLClassLoader(arrayOf(myLibUrl), javaClass.classLoader)
  }

  @Test
  fun `instance of class from a classloader can be deserialized`() {
    val barObject = cl.loadClass("foo.Bar").newInstance()

    val deserialized = wrapAndDeserialize(Any::class.java, barObject)

    assertThatExceptionOfType(IllegalStateException::class.java)
      .isThrownBy { deserialized.prepareData(javaClass.classLoader) }

    val newCl = URLClassLoader(arrayOf(myLibUrl), javaClass.classLoader)

    deserialized.prepareData(newCl)
    assertThat(deserialized.data.javaClass.name)
      .contains("foo.Bar")
  }

  @Test
  fun `proxy instance can be deserialized`() {
    val interfaceClass = cl.loadClass("foo.Baz")

    val invocationHandler = object : InvocationHandler, Serializable {
      override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?) = 0
    }

    var proxyInstance = Proxy.newProxyInstance(cl, arrayOf(interfaceClass), invocationHandler)
    val deserialized = wrapAndDeserialize(interfaceClass as Class<Any>, proxyInstance)


    assertThatExceptionOfType(IllegalStateException::class.java)
      .isThrownBy { deserialized.prepareData(javaClass.classLoader) }

    val newCl = URLClassLoader(arrayOf(myLibUrl), javaClass.classLoader)

    deserialized.prepareData(newCl)
    assertThat(deserialized.data.javaClass.interfaces)
      .extracting("name")
      .contains("foo.Baz")
  }

  @Test
  fun `ProjectSystemIds are re-used after deserialization`() {
    val id = ProjectSystemId("MyTest")
    val dataNodes = listOf(DataNode(Key.create(ProjectSystemId::class.java, 0), id, null),
                           DataNode(Key.create(ProjectSystemId::class.java, 0), id, null))

    val bos = ByteArrayOutputStream()
    ObjectOutputStream(bos).use { it.writeObject(dataNodes) }
    val bytes = bos.toByteArray()
    val deserializedList = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as List<DataNode<ProjectSystemId>>

    assertThat(deserializedList).hasSize(2)
    assertThat(deserializedList[0].data === deserializedList[1].data)
      .`as`("project system ID instances should be re-used")
      .isTrue()
  }

  @Test
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

    val dataNode = wrapAndDeserialize(Any::class.java, proxy)
    val counter = dataNode.data as Counter
    assertThat(counter.incrementAndGet()).isEqualTo(2)
  }

  private fun wrapAndDeserialize(clz: Class<Any>,
                                 barObject: Any): DataNode<Any> {
    val original = DataNode(Key.create(clz, 0), barObject, null)
    val bos = ByteArrayOutputStream()
    ObjectOutputStream(bos).use { it.writeObject(original) }
    val bytes = bos.toByteArray()
    return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as DataNode<Any>
  }
}

interface Counter {
  fun incrementAndGet(): Int
}


