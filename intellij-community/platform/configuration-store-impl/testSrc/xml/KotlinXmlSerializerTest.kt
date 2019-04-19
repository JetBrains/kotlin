// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.xml

import com.intellij.configurationStore.AState
import com.intellij.configurationStore.deserialize
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.annotations.MapAnnotation
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.Tag
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class KotlinXmlSerializerTest {
  @Test fun internalVar() {
    @Tag("bean")
    class Foo {
      var PLACES_MAP = ""
    }

    val data = Foo()
    data.PLACES_MAP = "new"
    testSerializer("""
    <bean>
      <option name="PLACES_MAP" value="new" />
    </bean>""", data)
  }

  @Tag("profile-state")
  private class VisibleTreeState : BaseState() {
    internal var foo by string()
  }

  private class VisibleTreeStateComponent : BaseState() {
    // we do not support private accessors
    @get:Property(surroundWithTag = false)
    @get:MapAnnotation(surroundWithTag = false, surroundKeyWithTag = false, surroundValueWithTag = false)
    var profileNameToState by map<String, VisibleTreeState>()

    fun getVisibleTreeState(profile: String) = profileNameToState.getOrPut(profile) {
      incrementModificationCount()
      VisibleTreeState()
    }
  }


  @Test fun `private map`() {
    val data = VisibleTreeStateComponent()
    data.getVisibleTreeState("new")
    testSerializer("""
<VisibleTreeStateComponent>
  <entry key="new">
    <profile-state />
  </entry>
</VisibleTreeStateComponent>""", data)
  }

  @Test fun floatProperty() {
    val state = AState()
    state.floatProperty = 3.4f
    testSerializer("""
    <AState>
      <option name="floatProperty" value="3.4" />
    </AState>
    """, state)
  }

  @Test fun nullInMap() {
    @Tag("bean")
    class Foo {
      @MapAnnotation(surroundWithTag = false, surroundKeyWithTag = false, surroundValueWithTag = false)
      var PLACES_MAP: TreeMap<String, PlaceSettings> = TreeMap()
    }

    val data = Foo()
    data.PLACES_MAP.put("", PlaceSettings())
    testSerializer("""
    <bean>
      <option name="PLACES_MAP">
        <entry key="">
          <PlaceSettings>
            <option name="IGNORE_POLICY" value="DEFAULT" />
          </PlaceSettings>
        </entry>
      </option>
    </bean>""", data)

    assertThat(JDOMUtil.load("""<bean>
          <option name="PLACES_MAP">
            <entry key="">
              <PlaceSettings>
                <option name="IGNORE_POLICY" />
              </PlaceSettings>
            </entry>
          </option>
        </bean>""").deserialize<Foo>().PLACES_MAP.get("")!!.IGNORE_POLICY).isEqualTo(IgnorePolicy.DEFAULT)

    val value = JDOMUtil.load("""<bean>
          <option name="PLACES_MAP">
            <entry key="">
              <PlaceSettings>
                <option name="SOME_UNKNOWN_VALUE" />
              </PlaceSettings>
            </entry>
          </option>
        </bean>""").deserialize<Foo>()
    assertThat(value).isNotNull()
    val placeSettings = value.PLACES_MAP.get("")
    assertThat(placeSettings).isNotNull()
    assertThat(placeSettings!!.IGNORE_POLICY).isEqualTo(IgnorePolicy.DEFAULT)
  }
}

private enum class IgnorePolicy(val text: String) {
  DEFAULT("Do not ignore"),
  TRIM_WHITESPACES("Trim whitespaces"),
  IGNORE_WHITESPACES("Ignore whitespaces"),
  IGNORE_WHITESPACES_CHUNKS("Ignore whitespaces and empty lines"),
  FORMATTING("Ignore formatting");
}

private data class PlaceSettings(var IGNORE_POLICY: IgnorePolicy = IgnorePolicy.DEFAULT)
