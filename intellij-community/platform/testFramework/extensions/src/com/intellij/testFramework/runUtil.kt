// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testFramework

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.impl.deserializeConfigurationFrom
import com.intellij.execution.impl.serializeConfigurationInto
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.testFramework.assertions.Assertions.assertThat
import org.jdom.Element
import org.jetbrains.annotations.TestOnly

// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@TestOnly
fun checkRunConfigurationSerialization(configuration: RunConfiguration, expected: String, factory: ConfigurationFactory?, project: Project) {
  val element = Element("state")
  serializeConfigurationInto(configuration, element)
  assertThat(element).isEqualTo(expected.trimIndent())

  if (factory != null) {
    val c2 = factory.createTemplateConfiguration(project)
    deserializeConfigurationFrom(c2, JDOMUtil.load(expected))
    checkRunConfigurationSerialization(c2, expected, null, project)
  }
}