// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.CollectionBean
import com.intellij.util.xmlb.annotations.XMap
import org.junit.Test

class StoredPropertyStateTest {
  private class Foo : BaseState() {
    var bar by property<AState>()
  }

  private class Foo2 : BaseState() {
    var bar: AState? by property(AState())
  }

  @Test
  fun `default null equals to bean with default property values`() {
    val f1 = Foo()
    val f2 = Foo()
    f2.bar = AState()

    assertThat(serialize(f1)).isNull()
    assertThat(serialize(f2)).isNull()
  }

  @Test
  fun `bean with default property values equals to default null`() {
    val f1 = Foo2()
    val f2 = Foo2()
    f2.bar = null

    assertThat(serialize(f1)).isNull()
    assertThat(serialize(f2)).isNull()
  }

  @Test
  fun test() {
    val state = AState()

    assertThat(state).isEqualTo(AState())

    assertThat(state.modificationCount).isEqualTo(0)
    assertThat(state.languageLevel).isNull()
    state.languageLevel = "foo"
    assertThat(state.modificationCount).isEqualTo(1)
    assertThat(state.languageLevel).isEqualTo("foo")
    assertThat(state.modificationCount).isEqualTo(1)

    assertThat(state).isNotEqualTo(AState())

    val newEqualState = AState()
    newEqualState.languageLevel = String("foo".toCharArray())
    assertThat(state).isEqualTo(newEqualState)

    assertThat(serialize(state)).isEqualTo("""<AState customName="foo" />""")
    assertThat(JDOMUtil.load("""<AState customName="foo" />""").deserialize(AState::class.java).languageLevel).isEqualTo("foo")
  }

  @Test
  fun childModificationCount() {
    val state = AState()
    assertThat(state.modificationCount).isEqualTo(0)
    val nestedState = NestedState()
    state.nestedComplex = nestedState
    assertThat(state.modificationCount).isEqualTo(1)

    nestedState.childProperty = "test"
    assertThat(state.modificationCount).isEqualTo(2)

    state.languageLevel = "11"
    assertThat(state.modificationCount).isEqualTo(3)

    state.languageLevel = null
    assertThat(state.modificationCount).isEqualTo(4)

    state.copyFrom(AState("foo", nestedState))
    @Suppress("USELESS_CAST")
    assertThat(state.languageLevel as String?).isEqualTo("foo")
    assertThat(state.modificationCount).isEqualTo(5)
  }

  @Test
  fun listModificationCount() {
    class TestOptions : BaseState() {
      @get:CollectionBean
      val pluginHosts by list<String>()
    }

    val state = TestOptions()
    val oldModificationCount = state.modificationCount

    val list = state.pluginHosts
    list.clear()
    list.addAll(listOf("foo"))
    assertThat(state.modificationCount).isNotEqualTo(oldModificationCount)
    assertThat(state.isEqualToDefault()).isFalse()

    val element = serialize(state)
    assertThat(element).isEqualTo("""
    <TestOptions>
      <pluginHosts>
        <item value="foo" />
      </pluginHosts>
    </TestOptions>""")
  }

  @Test
  fun `map modification count`() {
    class TestOptions : BaseState() {
      @get:XMap
      val foo by map<String, String>()
    }

    val state = TestOptions()
    var oldModificationCount = state.modificationCount

    val list = state.foo
    list.clear()
    list.put("a", "b")
    assertThat(state.modificationCount).isNotEqualTo(oldModificationCount)
    assertThat(state.isEqualToDefault()).isFalse()

    val element = serialize(state)
    assertThat(element).isEqualTo("""
    <TestOptions>
      <foo>
        <entry key="a" value="b" />
      </foo>
    </TestOptions>""")

    oldModificationCount = state.modificationCount
    list.clear()
    assertThat(state.modificationCount).isNotEqualTo(oldModificationCount)
    assertThat(state.isEqualToDefault()).isTrue()

    oldModificationCount = state.modificationCount
    list.put("a", "v")
    list.put("b", "v")
    assertThat(state.modificationCount).isNotEqualTo(oldModificationCount)
    assertThat(state.isEqualToDefault()).isFalse()

    oldModificationCount = state.modificationCount
    list.remove("a")
    assertThat(state.modificationCount).isNotEqualTo(oldModificationCount)
    assertThat(state.isEqualToDefault()).isFalse()
  }
}

internal class AState(languageLevel: String? = null, nestedComplex: NestedState? = null) : BaseState() {
  @get:Attribute("customName")
  var languageLevel by string(languageLevel)

  var bar by string()

  var property2 by property(0)

  var floatProperty by property(0.3f)

  var nestedComplex by property(nestedComplex)
}

internal class NestedState : BaseState() {
  var childProperty by string()
}