// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.xml

import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.annotations.MapAnnotation
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import gnu.trove.THashMap
import org.junit.Test
import java.util.*

internal class XmlSerializerOldMapAnnotationTest {
  @Test fun beanValueUsingSkipDefaultsFilter() {
    @Tag("bean")
    class Bean {
      @MapAnnotation(surroundWithTag = false, surroundKeyWithTag = false, surroundValueWithTag = false)
      var values: Map<String, BeanWithProperty> = THashMap()
    }

    val bean = Bean()
    testSerializer("<bean />", bean, SkipDefaultsSerializationFilter())
  }

  @Test fun mapAtTopLevel() {
    @Tag("bean")
    class BeanWithMapAtTopLevel {
      @Property(surroundWithTag = false)
      @MapAnnotation(surroundWithTag = false, surroundKeyWithTag = false, surroundValueWithTag = false)
      var map = LinkedHashMap<String, String>()

      var option: String? = null
    }

    val bean = BeanWithMapAtTopLevel()
    bean.map.put("a", "b")
    bean.option = "xxx"
    testSerializer("""
    <bean>
      <option name="option" value="xxx" />
      <entry key="a" value="b" />
    </bean>""", bean)
  }

  @Test fun notSurroundingKeyAndValue() {
    @Tag("bean")
    class Bean {
      @Tag("map")
      @MapAnnotation(surroundWithTag = false, entryTagName = "pair", surroundKeyWithTag = false, surroundValueWithTag = false)
      var MAP = LinkedHashMap<BeanWithPublicFields, BeanWithTextAnnotation>()
    }

    val bean = Bean()

    bean.MAP.put(BeanWithPublicFields(1, "a"), BeanWithTextAnnotation(2, "b"))
    bean.MAP.put(BeanWithPublicFields(3, "c"), BeanWithTextAnnotation(4, "d"))
    bean.MAP.put(BeanWithPublicFields(5, "e"), BeanWithTextAnnotation(6, "f"))

    testSerializer("""
    <bean>
      <map>
        <pair>
          <BeanWithPublicFields>
            <option name="INT_V" value="1" />
            <option name="STRING_V" value="a" />
          </BeanWithPublicFields>
          <BeanWithTextAnnotation>
            <option name="INT_V" value="2" />
            b
          </BeanWithTextAnnotation>
        </pair>
        <pair>
          <BeanWithPublicFields>
            <option name="INT_V" value="3" />
            <option name="STRING_V" value="c" />
          </BeanWithPublicFields>
          <BeanWithTextAnnotation>
            <option name="INT_V" value="4" />
            d
          </BeanWithTextAnnotation>
        </pair>
        <pair>
          <BeanWithPublicFields>
            <option name="INT_V" value="5" />
            <option name="STRING_V" value="e" />
          </BeanWithPublicFields>
          <BeanWithTextAnnotation>
            <option name="INT_V" value="6" />
            f
          </BeanWithTextAnnotation>
        </pair>
      </map>
    </bean>""", bean)
  }

  @Test fun beanWithMapWithSetValue() {
    @Tag("bean")
    class BeanWithMapWithSetValue {
      @MapAnnotation(entryTagName = "entry-tag", keyAttributeName = "key-attr", surroundWithTag = false)
      var values = LinkedHashMap<String, Set<String>>()
    }

    val bean = BeanWithMapWithSetValue()

    bean.values.put("a", linkedSetOf("first1", "second1"))
    bean.values.put("b", linkedSetOf("first2", "second2"))

    testSerializer("""
    <bean>
      <option name="values">
        <entry-tag key-attr="a">
          <value>
            <set>
              <option value="first1" />
              <option value="second1" />
            </set>
          </value>
        </entry-tag>
        <entry-tag key-attr="b">
          <value>
            <set>
              <option value="first2" />
              <option value="second2" />
            </set>
          </value>
        </entry-tag>
      </option>
    </bean>""", bean)
  }

  private class BeanWithMapWithAnnotations {
    @Property(surroundWithTag = false)
    @MapAnnotation(surroundWithTag = false, entryTagName = "option", keyAttributeName = "name", valueAttributeName = "value")
    var VALUES: MutableMap<String, String> = LinkedHashMap()

    init {
      VALUES.put("a", "1")
      VALUES.put("b", "2")
      VALUES.put("c", "3")
    }
  }

  @Test fun serializationWithAnnotations() {
    val bean = BeanWithMapWithAnnotations()
    testSerializer("<BeanWithMapWithAnnotations>\n  <option name=\"a\" value=\"1\" />\n  <option name=\"b\" value=\"2\" />\n  <option name=\"c\" value=\"3\" />\n</BeanWithMapWithAnnotations>", bean)
    bean.VALUES.clear()
    bean.VALUES.put("1", "a")
    bean.VALUES.put("2", "b")
    bean.VALUES.put("3", "c")

    testSerializer("<BeanWithMapWithAnnotations>\n  <option name=\"1\" value=\"a\" />\n  <option name=\"2\" value=\"b\" />\n  <option name=\"3\" value=\"c\" />\n</BeanWithMapWithAnnotations>", bean)
  }

  @Test fun `propertyWithoutSurroundingElement`() {
    @Tag("branch-storage")
    class BranchStorage {
      @Property(surroundWithTag = false)
      @MapAnnotation(keyAttributeName = "type")
      @JvmField
      var branches = THashMap<String, String>()
    }

    val bean = BranchStorage()
    bean.branches.put("branchName", "foo")

    testSerializer("""
      <branch-storage>
        <map>
          <entry type="branchName" value="foo" />
        </map>
      </branch-storage>
    """, bean, SkipDefaultsSerializationFilter())
  }
}