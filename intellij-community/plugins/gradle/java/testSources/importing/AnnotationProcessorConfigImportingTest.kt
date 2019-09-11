// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing

import com.intellij.compiler.CompilerConfiguration
import com.intellij.compiler.CompilerConfigurationImpl
import org.assertj.core.api.BDDAssertions.then
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Test

class AnnotationProcessorConfigImportingTest: GradleImportingTestCase() {

  @Test
  @TargetVersions("4.6+")
  fun `test annotation processor config imported in module per project mode`() {
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
      .hasSize(1)

    with (moduleProcessorProfiles[0]) {
      then(isEnabled).isTrue()
      then(isObtainProcessorsFromClasspath).isFalse()
      then(processorPath).contains("lombok")
      then(moduleNames).containsExactly("project")
    }

    importProjectUsingSingeModulePerGradleProject()

    val moduleProcessorProfilesAfterReImport = config.moduleProcessorProfiles
    then(moduleProcessorProfilesAfterReImport)
      .describedAs("Duplicate annotation processor profile should not appear")
      .hasSize(1)
  }

  @Test
  @TargetVersions("4.6+")
  fun `test annotation processor modification`() {
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
      .hasSize(1)

    importProjectUsingSingeModulePerGradleProject(
      GradleBuildScriptBuilderEx()
        .withJavaPlugin()
        .withMavenCentral()
        .addPostfix(
          """
      apply plugin: 'java'
      
      dependencies {
        compileOnly 'com.google.dagger:dagger:2.24'
        annotationProcessor 'com.google.dagger:dagger-compiler:2.24'
      }
    """.trimIndent()).generate());

    val modifiedProfiles = config.moduleProcessorProfiles

    then(modifiedProfiles)
      .describedAs("An annotation processor should be updated, not added")
      .hasSize(1)

    with (modifiedProfiles[0]) {
      then(isEnabled).isTrue()
      then(isObtainProcessorsFromClasspath).isFalse()
      then(processorPath)
        .describedAs("annotation processor config path should point to new annotation processor")
        .contains("dagger")
      then(moduleNames).containsExactly("project")
    }
  }

  @Test
  @TargetVersions("4.6+")
  fun `test annotation processor config imported in modules per source set mode`() {
    importProject(
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
      .hasSize(1)

    with (moduleProcessorProfiles[0]) {
      then(isEnabled).isTrue()
      then(isObtainProcessorsFromClasspath).isFalse()
      then(processorPath).contains("lombok")
      then(moduleNames).containsExactly("project.main")
    }
  }

  @Test
  @TargetVersions("4.6+")
  fun `test two different annotation processors`() {
    createProjectSubFile("settings.gradle", "include 'project1','project2'")
    importProject(
      GradleBuildScriptBuilderEx()
        .withMavenCentral()
        .addPostfix(
          """
            |  allprojects { apply plugin: 'java' }
            |  project("project1") {
            |      dependencies {
            |        compileOnly 'org.projectlombok:lombok:1.18.8'
            |        annotationProcessor 'org.projectlombok:lombok:1.18.8'
            |      }
            |  }
            |  
            |  project("project2") {
            |    dependencies {
            |        compileOnly 'com.google.dagger:dagger:2.24'
            |        annotationProcessor 'com.google.dagger:dagger-compiler:2.24'
            |    }
            |  }
    """.trimMargin()).generate());

    val config = CompilerConfiguration.getInstance(myProject) as CompilerConfigurationImpl
    val moduleProcessorProfiles = config.moduleProcessorProfiles

    then(moduleProcessorProfiles)
      .describedAs("Annotation processors profiles should be created correctly")
      .hasSize(2)
      .anyMatch {
        it.isEnabled && !it.isObtainProcessorsFromClasspath
        && it.processorPath.contains("lombok")
        && it.moduleNames == setOf("project.project1.main")
      }
      .anyMatch {
        it.isEnabled && !it.isObtainProcessorsFromClasspath
        && it.processorPath.contains("dagger")
        && it.moduleNames == setOf("project.project2.main")
      }
  }

  @Test
  @TargetVersions("4.6+")
   fun `test change modules included in processor profile`() {
       createProjectSubFile("settings.gradle", "include 'project1','project2'")
       importProject(
         GradleBuildScriptBuilderEx()
           .withMavenCentral()
           .addPostfix(
             """
            |  allprojects { apply plugin: 'java' }
            |  project("project1") {
            |      dependencies {
            |        compileOnly 'org.projectlombok:lombok:1.18.8'
            |        annotationProcessor 'org.projectlombok:lombok:1.18.8'
            |      }
            |  }
    """.trimMargin()).generate());

    then((CompilerConfiguration.getInstance(myProject) as CompilerConfigurationImpl)
           .moduleProcessorProfiles)
      .describedAs("Annotation processor profile includes wrong module")
      .extracting("moduleNames")
      .containsExactly(setOf("project.project1.main"))

    importProject(
      GradleBuildScriptBuilderEx()
        .withMavenCentral()
        .addPostfix(
          """
            |  allprojects { apply plugin: 'java' }
            |  project("project2") {
            |      dependencies {
            |        compileOnly 'org.projectlombok:lombok:1.18.8'
            |        annotationProcessor 'org.projectlombok:lombok:1.18.8'
            |      }
            |  }
    """.trimMargin()).generate());

    then((CompilerConfiguration.getInstance(myProject) as CompilerConfigurationImpl)
           .moduleProcessorProfiles)
         .describedAs("Annotation processor profile includes wrong module")
         .extracting("moduleNames")
         .containsExactly(setOf("project.project2.main"))
   }

  @Test
  @TargetVersions("4.6+")
  fun `test annotation processor with transitive deps`() {
    importProject(
      GradleBuildScriptBuilderEx()
        .withJavaPlugin()
        .withMavenCentral()
        .addPostfix(
          """
      apply plugin: 'java'
      
      dependencies {
        annotationProcessor 'junit:junit:4.12' // this is not an annotation processor, but has transitive deps
      }
    """.trimIndent()).generate());

    then((CompilerConfiguration.getInstance(myProject) as CompilerConfigurationImpl)
           .moduleProcessorProfiles[0]
           .processorPath)
      .describedAs("Annotation processor path should include junit and hamcrest")
      .contains("junit", "hamcrest")
  }
}