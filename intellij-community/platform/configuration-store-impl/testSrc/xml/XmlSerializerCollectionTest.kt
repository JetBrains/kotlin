// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("DEPRECATION")

package com.intellij.configurationStore.xml

import com.intellij.configurationStore.deserialize
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.util.JDOMExternalizableStringList
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.SmartList
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.annotations.AbstractCollection
import com.intellij.util.xmlb.annotations.CollectionBean
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import org.jdom.Element
import org.junit.Test

@Suppress("PropertyName")
internal class XmlSerializerCollectionTest {
  @Test fun jdomExternalizableStringList() {
    @Tag("b")
    class Bean3 {
      @Suppress("DEPRECATED_SYMBOL_WITH_MESSAGE")
      var list = JDOMExternalizableStringList()
    }

    val bean = Bean3()
    bean.list.add("\u0001one")
    bean.list.add("two")
    bean.list.add("three")
    testSerializer(
      """
      <b>
        <list>
          <item value="one" />
          <item value="two" />
          <item value="three" />
        </list>
      </b>""",
      bean, SkipDefaultsSerializationFilter())
  }

  @Test
  fun jdomExternalizableStringListWithoutClassAttribute() {
    val testList = arrayOf("foo", "bar")
    val element = Element("test")
    val listElement = Element("list")
    element.addContent(listElement)
    for (id in testList) {
      listElement.addContent(Element("item").setAttribute("itemvalue", id))
    }

    val result = SmartList<String>()
    JDOMExternalizableStringList.readList(result, element)
    assertThat(result).isEqualTo(testList.toList())
  }

  @Test fun collectionBean() {
    val bean = Bean4()
    bean.list.add("one")
    bean.list.add("two")
    bean.list.add("three")
    testSerializer("""
      <b>
        <list>
          <item value="one" />
          <item value="two" />
          <item value="three" />
        </list>
      </b>""", bean, SkipDefaultsSerializationFilter())
  }

  @Test fun collectionBeanReadJDOMExternalizableStringList() {
    @Suppress("DEPRECATED_SYMBOL_WITH_MESSAGE")
    val list = JDOMExternalizableStringList()
    list.add("one")
    list.add("two")
    list.add("three")

    val value = Element("value")
    list.writeExternal(value)
    val o = deserialize<Bean4>(Element("state").addContent(Element("option").setAttribute("name", "myList").addContent(value)))
    assertSerializer(o, "<b>\n" + "  <list>\n" + "    <item value=\"one\" />\n" + "    <item value=\"two\" />\n" + "    <item value=\"three\" />\n" + "  </list>\n" + "</b>", SkipDefaultsSerializationFilter())
  }

  @Test fun polymorphicArray() {
    @Tag("bean")
    class BeanWithPolymorphicArray {
      @AbstractCollection(elementTypes = [(BeanWithPublicFields::class), (BeanWithPublicFieldsDescendant::class)])
      var v = arrayOf<BeanWithPublicFields>()
    }

    val bean = BeanWithPolymorphicArray()

    testSerializer("<bean>\n  <option name=\"v\">\n    <array />\n  </option>\n</bean>", bean)

    bean.v = arrayOf(BeanWithPublicFields(), BeanWithPublicFieldsDescendant(), BeanWithPublicFields())

    testSerializer("""<bean>
  <option name="v">
    <array>
      <BeanWithPublicFields>
        <option name="INT_V" value="1" />
        <option name="STRING_V" value="hello" />
      </BeanWithPublicFields>
      <BeanWithPublicFieldsDescendant>
        <option name="NEW_S" value="foo" />
        <option name="INT_V" value="1" />
        <option name="STRING_V" value="hello" />
      </BeanWithPublicFieldsDescendant>
      <BeanWithPublicFields>
        <option name="INT_V" value="1" />
        <option name="STRING_V" value="hello" />
      </BeanWithPublicFields>
    </array>
  </option>
</bean>""", bean)
  }

  @Test fun xCollection() {
    val bean = BeanWithArrayWithoutTagName()
     testSerializer(
     """
      <BeanWithArrayWithoutTagName>
        <option name="foo">
          <option value="a" />
        </option>
      </BeanWithArrayWithoutTagName>""".trimIndent(), bean)
  }

  @Test fun arrayAnnotationWithElementTag() {
    @Tag("bean") class Bean {
      @AbstractCollection(elementTag = "vValue", elementValueAttribute = "v")
      var v = arrayOf("a", "b")
    }

    val bean = Bean()

    testSerializer("""
    <bean>
      <option name="v">
        <array>
          <vValue v="a" />
          <vValue v="b" />
        </array>
      </option>
    </bean>""", bean)

    bean.v = arrayOf("1", "2", "3")

    testSerializer("""
      <bean>
        <option name="v">
          <array>
            <vValue v="1" />
            <vValue v="2" />
            <vValue v="3" />
          </array>
        </option>
      </bean>""", bean)
  }

  @Test fun arrayWithoutTag() {
    @Tag("bean")
    class Bean {
      @XCollection(elementName = "vValue", valueAttributeName = "v")
      var v = arrayOf("a", "b")
      @Suppress("PropertyName", "unused")
      var INT_V = 1
    }

    val bean = Bean()

    testSerializer("""
    <bean>
      <option name="INT_V" value="1" />
      <option name="v">
        <vValue v="a" />
        <vValue v="b" />
      </option>
    </bean>""", bean)

    bean.v = arrayOf("1", "2", "3")

    testSerializer("""
    <bean>
      <option name="INT_V" value="1" />
      <option name="v">
        <vValue v="1" />
        <vValue v="2" />
        <vValue v="3" />
      </option>
    </bean>""", bean)
  }

  private data class BeanWithArray(@Suppress("ArrayInDataClass") var ARRAY_V: Array<String> = arrayOf("a", "b"))

  @Test fun array() {
    val bean = BeanWithArray()
    testSerializer(
      "<BeanWithArray>\n  <option name=\"ARRAY_V\">\n    <array>\n      <option value=\"a\" />\n      <option value=\"b\" />\n    </array>\n  </option>\n</BeanWithArray>",
      bean)

    bean.ARRAY_V = arrayOf("1", "2", "3", "")
    testSerializer(
      "<BeanWithArray>\n  <option name=\"ARRAY_V\">\n    <array>\n      <option value=\"1\" />\n      <option value=\"2\" />\n      <option value=\"3\" />\n      <option value=\"\" />\n    </array>\n  </option>\n</BeanWithArray>",
      bean)
  }

  @Test
  fun `string set with one value as default`() {
    class Bean : BaseState() {
      @get:XCollection(style = XCollection.Style.v2)
      val names by stringSet("test")
    }

    val bean = Bean()
    assertThat(bean.isEqualToDefault()).isTrue()
    testSerializer(
      "<Bean />",
      bean)

    bean.names.clear()
    bean.intIncrementModificationCount()
    assertThat(bean.isEqualToDefault()).isFalse()
    testSerializer(
      "<Bean>\n" +
      "  <names />\n" +
      "</Bean>",
      bean)
  }
}

@Tag("b")
private class Bean4 {
  @CollectionBean
  val list = SmartList<String>()
}

private class BeanWithArrayWithoutTagName {
  @XCollection
  var foo = arrayOf("a")
}