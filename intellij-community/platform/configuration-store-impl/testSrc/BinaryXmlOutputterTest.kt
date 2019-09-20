// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class BinaryXmlOutputterTest {
  @Test fun noAttributes() {
    test("""<foo />""")
  }

  @Test fun textWithSpaces() {
    test("""
      <foo>
        <bar>Hello world</bar>
      </foo>""".trimMargin())
  }

  @Test fun attributes() {
    test("""
    <state>
      <component name="CopyrightManager">
        <copyright>
          <option name="myName" value="Foo" />
          <option name="notice" value="where" />
        </copyright>
      </component>
      <component name="InspectionProjectProfileManager">
        <profile version="1.0">
          <option name="myName" value="Project Default" />
          <inspection_tool class="AntDuplicateTargetsInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="AntMissingPropertiesFileInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="AntResolveInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="ArgNamesErrorsInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="ArgNamesWarningsInspection" enabled="false" level="WARNING" enabled_by_default="false" />
          <inspection_tool class="AroundAdviceStyleInspection" enabled="false" level="WARNING" enabled_by_default="false" />
          <inspection_tool class="DeclareParentsInspection" enabled="false" level="ERROR" enabled_by_default="false" />
          <inspection_tool class="EmptyEventHandler" enabled="false" level="WARNING" enabled_by_default="false" />
          <inspection_tool class="PointcutMethodStyleInspection" enabled="false" level="WARNING" enabled_by_default="false" />
        </profile>
        <version value="1.0" />
      </component>
      <component name="ProjectLevelVcsManager" settingsEditedManually="false" />
      <component name="masterDetails">
        <states>
          <state key="Copyright.UI">
            <settings>
              <last-edited>Foo</last-edited>
              <splitter-proportions>
                <option name="proportions">
                  <list>
                    <option value="0.2" />
                  </list>
                </option>
              </splitter-proportions>
            </settings>
          </state>
          <state key="ProjectJDKs.UI">
            <settings>
              <last-edited>1.4</last-edited>
              <splitter-proportions>
                <option name="proportions">
                  <list>
                    <option value="0.2" />
                  </list>
                </option>
              </splitter-proportions>
            </settings>
          </state>
        </states>
      </component>
    </state>""")
  }

  private fun test(xml: String) {
    val byteOut = BufferExposingByteArrayOutputStream()
    byteOut.use {
      serializeElementToBinary(JDOMUtil.load(xml), it)
    }

    val xmlAfter = JDOMUtil.writeElement(byteOut.toByteArray().inputStream().use { deserializeElementFromBinary(it) })

    assertThat(xml.trimIndent()).isEqualTo(xmlAfter)
  }
}