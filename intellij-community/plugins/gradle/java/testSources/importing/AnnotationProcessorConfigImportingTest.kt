// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.compiler.CompilerConfiguration
import com.intellij.compiler.CompilerConfigurationImpl
import org.assertj.core.api.BDDAssertions.then
import org.junit.Test

class AnnotationProcessorConfigImportingTest: GradleImportingTestCase() {

  @Test
  fun `test annotation processor config imported`() {
    importProjectUsingSingeModulePerGradleProject(
      GradleBuildScriptBuilderEx()
        .withJavaPlugin()
        .withMavenCentral()
        .addPostfix(
      """
      apply plugin: 'java'
      
      dependencies {
        compileOnly 'org.projectlombok:lombok:1.18.8'
        annotationProcessor 'org.projectlombok:lombok:1.18.8'
      }
    """.trimIndent()).generate());

    val config = CompilerConfiguration.getInstance(myProject) as CompilerConfigurationImpl
    val moduleProcessorProfiles = config.moduleProcessorProfiles

    then(moduleProcessorProfiles)
      .describedAs("An annotation processor profile should be created for Gradle module")
      .isNotEmpty
  }

  // fun `test repeated import does not cause multiple profiles`() {}
  // fun `test annotation processor modification`() {}
  // fun `test two different annotation processors`() {}
  // fun `test annotation processor with transitive deps`() {}
}