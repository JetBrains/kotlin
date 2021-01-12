// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.xml

import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import org.junit.Test
import java.util.*

class XmlSerializerListTest {
  @Test
  fun notFinalField() {
    @Tag("bean")
    class Bean {
      @JvmField
      var values = arrayListOf("a", "b", "w")
    }

    val bean = Bean()
    check(bean) {
      bean.values = it
    }
  }

  @Test
  fun `empty list`() {
    @Tag("bean")
    class Bean {
      @JvmField
      var values = emptyList<String>()
    }

    val data = Bean()
    data.values = listOf("foo")
    testSerializer("""
    <bean>
      <option name="values">
        <list>
          <option value="foo" />
        </list>
      </option>
    </bean>""", data)
  }

  @Test
  fun `empty java list`() {
    @Tag("bean")
    class Bean {
      @JvmField
      var values = Collections.emptyList<String>()
    }

    val data = Bean()
    data.values = listOf("foo")
    testSerializer("""
    <bean>
      <option name="values">
        <list>
          <option value="foo" />
        </list>
      </option>
    </bean>""", data)
  }

  @Test
  fun notFinalProperty() {
    @Tag("bean")
    class Bean {
      var values = arrayListOf("a", "b", "w")
    }

    val bean = Bean()
    check(bean) {
      bean.values = it
    }
  }

  @Test
  fun finalField() {
    @Tag("bean")
    class Bean {
      @JvmField
      val values = arrayListOf("a", "b", "w")
    }

    val bean = Bean()
    check(bean) {
      bean.values.clear()
      bean.values.addAll(it)
    }
  }

  @Test
  fun finalProperty() {
    @Tag("bean")
    class Bean {
      @OptionTag
      val values = arrayListOf("a", "b", "w")
    }

    val bean = Bean()
    check(bean) {
      bean.values.clear()
      bean.values.addAll(it)
    }
  }

  private data class SpecSource(var pathOrUrl: String? = null)

  @Test
  fun `final property and style v2`() {
    @Tag("bean")
    class Bean : BaseState() {
      @get:XCollection(style = XCollection.Style.v2)
      val specSources by list<SpecSource>()
    }

    val bean = Bean()

    testSerializer("""<bean />""", bean)

    bean.specSources.clear()
    bean.specSources.addAll(listOf(SpecSource("foo"), SpecSource("bar")))

    testSerializer("""
      <bean>
        <specSources>
          <SpecSource>
            <option name="pathOrUrl" value="foo" />
          </SpecSource>
          <SpecSource>
            <option name="pathOrUrl" value="bar" />
          </SpecSource>
        </specSources>
      </bean>""", bean)
  }

  @Test
  fun finalPropertyWithoutWrapping() {
    @Tag("bean")
    class Bean {
      @XCollection
      val values = arrayListOf("a", "b", "w")
    }

    val bean = Bean()
    testSerializer("""
    <bean>
      <option name="values">
        <option value="a" />
        <option value="b" />
        <option value="w" />
      </option>
    </bean>""", bean)

    bean.values.clear()
    bean.values.addAll(listOf("1", "2", "3"))

    testSerializer("""
    <bean>
      <option name="values">
        <option value="1" />
        <option value="2" />
        <option value="3" />
      </option>
    </bean>""", bean)
  }

  private fun <T : Any> check(bean: T, setter: (values: ArrayList<String>) -> Unit) {
    testSerializer("""
      <bean>
        <option name="values">
          <list>
            <option value="a" />
            <option value="b" />
            <option value="w" />
          </list>
        </option>
      </bean>""", bean)
    setter(arrayListOf("1", "2", "3"))

    testSerializer("""
      <bean>
        <option name="values">
          <list>
            <option value="1" />
            <option value="2" />
            <option value="3" />
          </list>
        </option>
      </bean>""", bean)
  }
}