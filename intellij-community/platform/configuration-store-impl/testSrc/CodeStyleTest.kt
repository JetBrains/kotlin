// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.util.JDOMUtil
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.util.containers.ContainerUtil
import org.jdom.Element
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

internal class CodeStyleTest {
  companion object {
    @JvmField
    @ClassRule
    val projectRule = ProjectRule(runPostStartUpActivities = false)
  }

  @JvmField
  @Rule
  val disposableRule = DisposableRule()

  @Test
  fun `do not remove unknown`() {
    val settings = CodeStyle.createTestSettings()
    val loaded = """
    <code_scheme name="testSchemeName" version="${CodeStyleSettings.CURR_VERSION}">
      <UnknownDoNotRemoveMe>
        <option name="ALIGN_OBJECT_PROPERTIES" value="2" />
      </UnknownDoNotRemoveMe>
      <codeStyleSettings language="CoffeeScript">
        <option name="KEEP_SIMPLE_METHODS_IN_ONE_LINE" value="true" />
      </codeStyleSettings>
      <codeStyleSettings language="DB2">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="Derby">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="Gherkin">
        <indentOptions>
          <option name="USE_TAB_CHARACTER" value="true" />
        </indentOptions>
      </codeStyleSettings>
      <codeStyleSettings language="H2">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="HSQLDB">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="MySQL">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="Oracle">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="PostgreSQL">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="SQL">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="SQLite">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="Sybase">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
      <codeStyleSettings language="TSQL">
        <option name="KEEP_LINE_BREAKS" value="false" />
      </codeStyleSettings>
    </code_scheme>""".trimIndent()
    settings.readExternal(JDOMUtil.load(loaded))

    val serialized = Element("code_scheme").setAttribute("name", "testSchemeName")
    settings.writeExternal(serialized)
    assertThat(serialized).isEqualTo(loaded)
  }

  @Test fun `do not duplicate known extra sections`() {
    val newProvider: CodeStyleSettingsProvider = object : CodeStyleSettingsProvider() {
      override fun createCustomSettings(settings: CodeStyleSettings?): CustomCodeStyleSettings {
        return object : CustomCodeStyleSettings("NewComponent", settings) {
          override fun getKnownTagNames(): List<String> {
            return ContainerUtil.concat(super.getKnownTagNames(), listOf("NewComponent-extra"))
          }

          override fun writeExternal(parentElement: Element?, parentSettings: CustomCodeStyleSettings) {
            super.writeExternal(parentElement, parentSettings)
            writeMain(parentElement)
            writeExtra(parentElement)
          }

          private fun writeMain(parentElement: Element?) {
            var extra = parentElement!!.getChild(tagName)
            if (extra == null) {
              extra = Element(tagName)
              parentElement.addContent(extra)
            }

            val option = Element("option")
            option.setAttribute("name", "MAIN")
            option.setAttribute("value", "3")
            extra.addContent(option)
          }
          private fun writeExtra(parentElement: Element?) {
            val extra = Element("NewComponent-extra")
            val option = Element("option")
            option.setAttribute("name", "EXTRA")
            option.setAttribute("value", "3")
            extra.addContent(option)
            parentElement!!.addContent(extra)
          }
        }
      }
    }

    ExtensionTestUtil.maskExtensions(CodeStyleSettingsProvider.EXTENSION_POINT_NAME, listOf(newProvider), disposableRule.disposable)
    val settings = CodeStyle.createTestSettings()
    fun text(param: String): String {
      return """
      <code_scheme name="testSchemeName" version="${CodeStyleSettings.CURR_VERSION}">
        <NewComponent>
          <option name="MAIN" value="${param}" />
        </NewComponent>
        <NewComponent-extra>
          <option name="EXTRA" value="${param}" />
        </NewComponent-extra>
        <codeStyleSettings language="CoffeeScript">
          <option name="KEEP_SIMPLE_METHODS_IN_ONE_LINE" value="true" />
        </codeStyleSettings>
        <codeStyleSettings language="DB2">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="Derby">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="Gherkin">
          <indentOptions>
            <option name="USE_TAB_CHARACTER" value="true" />
          </indentOptions>
        </codeStyleSettings>
        <codeStyleSettings language="H2">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="HSQLDB">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="MySQL">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="Oracle">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="PostgreSQL">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="SQL">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="SQLite">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="Sybase">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
        <codeStyleSettings language="TSQL">
          <option name="KEEP_LINE_BREAKS" value="false" />
        </codeStyleSettings>
      </code_scheme>""".trimIndent()
    }

    settings.readExternal(JDOMUtil.load(text("2")))

    val serialized = Element("code_scheme").setAttribute("name", "testSchemeName")
    settings.writeExternal(serialized)
    assertThat(serialized).isEqualTo(text("3"))
  }

  @Test fun `reset deprecations`() {
    val settings = CodeStyle.createTestSettings()
    val initial = """
    <code_scheme name="testSchemeName" version="${CodeStyleSettings.CURR_VERSION}">
      <option name="RIGHT_MARGIN" value="64" />
      <option name="USE_FQ_CLASS_NAMES_IN_JAVADOC" value="false" />
    </code_scheme>""".trimIndent()
    val expected = """
    <code_scheme name="testSchemeName" version="${CodeStyleSettings.CURR_VERSION}">
      <option name="RIGHT_MARGIN" value="64" />
    </code_scheme>""".trimIndent()

    settings.readExternal(JDOMUtil.load(initial))
    settings.resetDeprecatedFields()

    val serialized = Element("code_scheme").setAttribute("name", "testSchemeName")
    settings.writeExternal(serialized)
    assertThat(serialized).isEqualTo(expected)
  }
}