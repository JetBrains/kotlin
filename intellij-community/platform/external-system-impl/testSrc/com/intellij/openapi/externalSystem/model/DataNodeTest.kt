// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.model

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import com.intellij.serialization.ObjectSerializer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.BDDAssertions.then
import org.junit.Before
import org.junit.Test
import java.io.Serializable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.URLClassLoader

class DataNodeTest {
  lateinit var classLoader: ClassLoader
  private val libUrl = javaClass.classLoader.getResource("dataNodeTest/lib.jar")

  @Before
  fun setUp() {
    classLoader = URLClassLoader(arrayOf(libUrl), javaClass.classLoader)
  }

  @Test
  fun `instance of class from a classloader can be deserialized`() {
    val barObject = classLoader.loadClass("foo.Bar").newInstance()
    val deserialized = wrapAndDeserialize(barObject, URLClassLoader(arrayOf(libUrl)))
    assertThat(deserialized.data.javaClass.name).isEqualTo("foo.Bar")
  }

  @Test
  fun `instance of class from a our own classloader can be deserialized`() {
    class Container {
      @JvmField
      var bar: Any? = null
    }

    val barClass = classLoader.loadClass("foo.Bar")
    val out = BufferExposingByteArrayOutputStream()
    val container = Container()
    container.bar = barClass.newInstance()
    ObjectSerializer.instance.writeList(listOf(container), Any::class.java, out, createCacheWriteConfiguration())
    // ClassNotFoundException because custom logic must be applied only to DataNode classes, but foo.Bar it is part of our test Container class
    assertThatThrownBy {
      ObjectSerializer.instance.readList(Any::class.java, out.toByteArray(),
                                         createCacheReadConfiguration(logger<DataNodeTest>(), testOnlyClassLoader = classLoader))
    }
      .hasCause(ClassNotFoundException("foo.Bar"))
  }

  // well, proxy cannot be serialized because on deserialize we need class
  fun `proxy instance can be deserialized`() {
    val interfaceClass = classLoader.loadClass("foo.Baz")

    val invocationHandler = InvocationHandler { _, _, _ -> 0 }

    val proxyInstance = Proxy.newProxyInstance(classLoader, arrayOf(interfaceClass), invocationHandler)
    @Suppress("UNCHECKED_CAST")
    val deserialized = wrapAndDeserialize(proxyInstance, URLClassLoader(arrayOf(libUrl)))
    assertThat(deserialized.data.javaClass.interfaces)
      .extracting("name")
      .contains("foo.Baz")
  }

  @Test
  fun `ProjectSystemIds are re-used after deserialization`() {
    val id = ProjectSystemId("MyTest")

    val dataNodes = listOf(DataNode(Key.create(ProjectSystemId::class.java, 0), id, null),
                           DataNode(Key.create(ProjectSystemId::class.java, 0), id, null))

    val out = BufferExposingByteArrayOutputStream()
    ObjectSerializer.instance.writeList(dataNodes, DataNode::class.java, out, createCacheWriteConfiguration())
    val bytes = out.toByteArray()
    val deserializedList = ObjectSerializer.instance.readList(DataNode::class.java, bytes, createDataNodeReadConfiguration { name, _ -> javaClass.classLoader.loadClass(name) })

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

    val dataNode = wrapAndDeserialize(proxy, javaClass.classLoader)
    val counter = dataNode.data as Counter
    assertThat(counter.incrementAndGet()).isEqualTo(2)
  }

  @Test
  fun `test DataNode visit allows children modification`() {
    val key = Key.create(Object::class.java, 0)
    val parent = DataNode(key, Object(), null)
    val child1 = DataNode(key, Object(), parent)
    val child2 = DataNode(key, Object(), parent)
    with(parent) {
      addChild(child1)
      addChild(child2)
    }

    parent.visit { if (it == child1) { it.clear(true) } }

    then(parent.children).containsExactly(child2)
  }

  private fun wrapAndDeserialize(barObject: Any, classLoader: ClassLoader): DataNode<*> {
    val original = DataNode(Key.create(barObject.javaClass, 0), barObject, null)
    val bytes = ObjectSerializer.instance.writeAsBytes(original, createCacheWriteConfiguration())
    return ObjectSerializer.instance.read(DataNode::class.java, bytes, createCacheReadConfiguration(logger<DataNodeTest>(), testOnlyClassLoader = classLoader))
  }
}

private interface Counter {
  fun incrementAndGet(): Int
}


