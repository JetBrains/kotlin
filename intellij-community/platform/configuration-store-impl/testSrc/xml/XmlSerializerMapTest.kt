// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("PropertyName")

package com.intellij.configurationStore.xml

import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap
import org.junit.Test
import java.util.*

internal class XmlSerializerMapTest {
  @Test
  fun `empty map`() {
    @Tag("bean")
    class Bean {
      @JvmField
      var values = emptyMap<String, String>()
    }

    val data = Bean()
    data.values = mapOf("foo" to "boo")
    testSerializer("""
        <bean>
          <option name="values">
            <map>
              <entry key="foo" value="boo" />
            </map>
          </option>
        </bean>""", data)
  }

  @Test fun mapAtTopLevel() {
    @Tag("bean")
    class BeanWithMapAtTopLevel {
      @Property(surroundWithTag = false)
      @XMap
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

  @Test fun propertyElementName() {
    @Tag("bean")
    class Bean {
      @XMap
      var map = LinkedHashMap<String, String>()
    }

    val bean = Bean()
    bean.map.put("a", "b")
    testSerializer("""
    <bean>
      <map>
        <entry key="a" value="b" />
      </map>
    </bean>""", bean)
  }

  @Test fun notSurroundingKeyAndValue() {
    @Tag("bean")
    class Bean {
      @XMap(propertyElementName = "map")
      var MAP = LinkedHashMap<BeanWithPublicFields, BeanWithTextAnnotation>()
    }

    val bean = Bean()

    bean.MAP.put(BeanWithPublicFields(1, "a"), BeanWithTextAnnotation(2, "b"))
    bean.MAP.put(BeanWithPublicFields(3, "c"), BeanWithTextAnnotation(4, "d"))
    bean.MAP.put(BeanWithPublicFields(5, "e"), BeanWithTextAnnotation(6, "f"))

    testSerializer("""
    <bean>
      <map>
        <entry>
          <BeanWithPublicFields>
            <option name="INT_V" value="1" />
            <option name="STRING_V" value="a" />
          </BeanWithPublicFields>
          <BeanWithTextAnnotation>
            <option name="INT_V" value="2" />
            b
          </BeanWithTextAnnotation>
        </entry>
        <entry>
          <BeanWithPublicFields>
            <option name="INT_V" value="3" />
            <option name="STRING_V" value="c" />
          </BeanWithPublicFields>
          <BeanWithTextAnnotation>
            <option name="INT_V" value="4" />
            d
          </BeanWithTextAnnotation>
        </entry>
        <entry>
          <BeanWithPublicFields>
            <option name="INT_V" value="5" />
            <option name="STRING_V" value="e" />
          </BeanWithPublicFields>
          <BeanWithTextAnnotation>
            <option name="INT_V" value="6" />
            f
          </BeanWithTextAnnotation>
        </entry>
      </map>
    </bean>""", bean)
  }

  @Test fun serialization() {
    @Tag("bean")
    class BeanWithMap {
      var VALUES: MutableMap<String, String> = LinkedHashMap()

      init {
        VALUES.put("a", "1")
        VALUES.put("b", "2")
        VALUES.put("c", "3")
      }
    }

    val bean = BeanWithMap()

    testSerializer("""
    <bean>
      <option name="VALUES">
        <map>
          <entry key="a" value="1" />
          <entry key="b" value="2" />
          <entry key="c" value="3" />
        </map>
      </option>
    </bean>""", bean)
    bean.VALUES.clear()
    bean.VALUES.put("1", "a")
    bean.VALUES.put("2", "b")
    bean.VALUES.put("3", "c")

    testSerializer("""
    <bean>
    <option name="VALUES">
      <map>
        <entry key="1" value="a" />
        <entry key="2" value="b" />
        <entry key="3" value="c" />
      </map>
    </option>
  </bean>""", bean)
  }

  @Test fun withBeanValue() {
    class BeanWithMapWithBeanValue {
      var VALUES: MutableMap<String, BeanWithProperty> = LinkedHashMap()
    }

    val bean = BeanWithMapWithBeanValue()

    bean.VALUES.put("a", BeanWithProperty("James"))
    bean.VALUES.put("b", BeanWithProperty("Bond"))
    bean.VALUES.put("c", BeanWithProperty("Bill"))

    testSerializer("""
    <BeanWithMapWithBeanValue>
      <option name="VALUES">
        <map>
          <entry key="a">
            <value>
              <BeanWithProperty>
                <option name="name" value="James" />
              </BeanWithProperty>
            </value>
          </entry>
          <entry key="b">
            <value>
              <BeanWithProperty>
                <option name="name" value="Bond" />
              </BeanWithProperty>
            </value>
          </entry>
          <entry key="c">
            <value>
              <BeanWithProperty>
                <option name="name" value="Bill" />
              </BeanWithProperty>
            </value>
          </entry>
        </map>
      </option>
    </BeanWithMapWithBeanValue>""", bean)
  }

  @Test fun setKeysInMap() {
    @Tag("bean")
    class BeanWithSetKeysInMap {
      var myMap = LinkedHashMap<Collection<String>, String>()
    }

    val bean = BeanWithSetKeysInMap()
    bean.myMap.put(LinkedHashSet(Arrays.asList("a", "b", "c")), "letters")
    bean.myMap.put(LinkedHashSet(Arrays.asList("1", "2", "3")), "numbers")

    val bb = testSerializer("""
      <bean>
      <option name="myMap">
        <map>
          <entry value="letters">
            <key>
              <set>
                <option value="a" />
                <option value="b" />
                <option value="c" />
              </set>
            </key>
          </entry>
          <entry value="numbers">
            <key>
              <set>
                <option value="1" />
                <option value="2" />
                <option value="3" />
              </set>
            </key>
          </entry>
        </map>
      </option>
    </bean>""", bean)

    for (collection in bb.myMap.keys) {
      assertThat(collection).isInstanceOf(Set::class.java)
    }
  }

  @Test
  fun nestedMapAndFinalFieldWithoutAnnotation() {
    @Tag("bean")
    class MapMap {
      // do not add store annotations - this test also checks that map field without annotation is supported
      @JvmField
      val foo: MutableMap<String, TreeMap<Long, String>> = TreeMap()
    }

    val bean = MapMap()
    bean.foo.put("bar", TreeMap(mapOf(12L to "22")))
    testSerializer("""
    <bean>
      <option name="foo">
        <map>
          <entry key="bar">
            <value>
              <map>
                <entry key="12" value="22" />
              </map>
            </value>
          </entry>
        </map>
      </option>
    </bean>
    """, bean)
  }
}