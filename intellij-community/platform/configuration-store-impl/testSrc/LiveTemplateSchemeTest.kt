// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.codeInsight.template.impl.TemplateSettings
import com.intellij.configurationStore.schemeManager.SchemeManagerFactoryBase
import com.intellij.testFramework.ProjectRule
import com.intellij.testFramework.rules.InMemoryFsRule
import com.intellij.util.io.readText
import com.intellij.util.io.write
import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class LiveTemplateSchemeTest {
  companion object {
    @JvmField
    @ClassRule
    val projectRule = ProjectRule()
  }

  @JvmField
  @Rule
  val fsRule = InMemoryFsRule()

  // https://youtrack.jetbrains.com/issue/IDEA-155623#comment=27-1721029
  @Test fun `do not remove unknown context`() {
    val schemeFile = fsRule.fs.getPath("templates/Groovy.xml")
    val schemeManagerFactory = SchemeManagerFactoryBase.TestSchemeManagerFactory(fsRule.fs.getPath(""))
    val schemeData = """
    <templateSet group="Groovy">
      <template name="serr" value="System.err.println(&quot;$\END$&quot;)dwed" description="Prints a string to System.errwefwe" toReformat="true" toShortenFQNames="true" deactivated="true">
        <context>
          <option name="GROOVY_STATEMENT" value="false" />
          <option name="__DO_NOT_DELETE_ME__" value="true" />
        </context>
      </template>
    </templateSet>""".trimIndent()

    schemeFile.write(schemeData)

    TemplateSettings(schemeManagerFactory)
    schemeManagerFactory.save()
    assertThat(schemeFile.readText()).isEqualTo(schemeData)
  }
}