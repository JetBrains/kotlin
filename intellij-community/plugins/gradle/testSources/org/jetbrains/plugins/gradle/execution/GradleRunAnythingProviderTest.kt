// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.execution

import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx
import org.jetbrains.plugins.gradle.importing.GradleSettingScriptBuilder
import org.junit.Test

class GradleRunAnythingProviderTest : GradleRunAnythingProviderTestCase() {

  override fun getTestsTempDir() = "tmp${System.currentTimeMillis()}"

  @Test
  fun `test class completion`() {
    createTestJavaClass("ClassA")
    createTestJavaClass("ClassB")
    createTestJavaClass("ClassC")
    createTestJavaClass("ClassD")
    createTestJavaClass("ClassE")
    createTestJavaClass("ClassF")
    createTestJavaClass("ClassG")
    val buildScript = GradleBuildScriptBuilderEx()
      .withJavaPlugin()
      .withJUnit("4.12")
    importProject(buildScript.generate())

    val wcCompletions = arrayOf(
      "gradle test --tests *ClassA",
      "gradle test --tests *ClassB",
      "gradle test --tests *ClassC",
      "gradle test --tests *ClassD",
      "gradle test --tests *ClassE",
      "gradle test --tests *ClassF",
      "gradle test --tests *ClassG"
    )
    val wcFqnCompletions = arrayOf(
      "gradle test --tests *.ClassA",
      "gradle test --tests *.ClassB",
      "gradle test --tests *.ClassC",
      "gradle test --tests *.ClassD",
      "gradle test --tests *.ClassE",
      "gradle test --tests *.ClassF",
      "gradle test --tests *.ClassG"
    )
    val fqnCompletions = arrayOf(
      "gradle test --tests org.jetbrains.ClassA",
      "gradle test --tests org.jetbrains.ClassB",
      "gradle test --tests org.jetbrains.ClassC",
      "gradle test --tests org.jetbrains.ClassD",
      "gradle test --tests org.jetbrains.ClassE",
      "gradle test --tests org.jetbrains.ClassF",
      "gradle test --tests org.jetbrains.ClassG"
    )
    withVariantsFor("gradle test ") { assertContains(it, "gradle test --tests") }
    withVariantsFor("gradle test -") { assertContains(it, "gradle test --tests") }
    withVariantsFor("gradle test --") { assertContains(it, "gradle test --tests") }
    withVariantsFor("gradle test --t") { assertContains(it, "gradle test --tests") }
    withVariantsFor("gradle test --tests ") { assertContains(it, *wcCompletions) }
    withVariantsFor("gradle test --tests *") { assertContains(it, *wcCompletions) }
    withVariantsFor("gradle test --tests *.") { assertContains(it, *wcFqnCompletions) }
    withVariantsFor("gradle test --tests *.Class") { assertContains(it, *wcFqnCompletions) }
    withVariantsFor("gradle test --tests org.jetbrains.") { assertContains(it, *fqnCompletions) }
    withVariantsFor("gradle test --tests org.jetbrains.Class") { assertContains(it, *fqnCompletions) }
  }

  @Test
  fun `test regular project`() {
    withVariantsFor("") {
      assertContains(it, getGradleOptions(), !getCommonTasks(), !getCommonTasks(":"))
    }
  }

  @Test
  fun `test single project`() {
    importProject(GradleBuildScriptBuilderEx().generate())
    withVariantsFor("") {
      assertContains(it, getGradleOptions(), getCommonTasks(), getCommonTasks(":"))
    }
    importProject(GradleBuildScriptBuilderEx().withTask("my-task").generate())
    withVariantsFor("") {
      assertContains(it, getGradleOptions(), getCommonTasks(), getCommonTasks(":"))
      assertContains(it, "my-task", ":my-task")
    }
    withVariantsFor("wrapper ") {
      assertContains(it, "wrapper my-task", "wrapper :my-task", !"wrapper wrapper", !"wrapper :wrapper")
    }
    withVariantsFor("my-task ") {
      assertContains(it, getGradleOptions("my-task "), getCommonTasks("my-task "), getCommonTasks("my-task :"))
      assertContains(it, !"my-task my-task", !"my-task :my-task")
    }
    withVariantsFor(":my-task ") {
      assertContains(it, getGradleOptions(":my-task "), getCommonTasks(":my-task "), getCommonTasks(":my-task :"))
      assertContains(it, !":my-task my-task", !":my-task :my-task")
    }
  }

  @Test
  fun `test multi-module project`() {
    createProjectSubFile("build.gradle", GradleBuildScriptBuilderEx().withTask("taskP").generate())
    createProjectSubFile("module/build.gradle", GradleBuildScriptBuilderEx().withTask("taskM").generate())
    createProjectSubFile("composite/build.gradle", GradleBuildScriptBuilderEx().withTask("taskC").generate())
    createProjectSubFile("composite/module/build.gradle", GradleBuildScriptBuilderEx().withTask("taskCM").generate())
    createProjectSubFile("settings.gradle", GradleSettingScriptBuilder("project").withModule("module").withBuild("composite").generate())
    createProjectSubFile("composite/settings.gradle", GradleSettingScriptBuilder("composite").withModule("module").generate())
    importProject()
    withVariantsFor("") {
      assertContains(it, getGradleOptions())
      assertContains(it, getRootProjectTasks(), getRootProjectTasks(":"), !getRootProjectTasks(":module:"))
      assertContains(it, getCommonTasks(), getCommonTasks(":"), getCommonTasks(":module:"))
      assertContains(it, "taskP", ":taskP", !":module:taskP")
      assertContains(it, "taskM", !":taskM", ":module:taskM")
      assertContains(it, !"taskC", !":taskC", !":module:taskC")
      assertContains(it, !"taskCM", !":taskCM", !":module:taskCM")
    }
    withVariantsFor("", "project") {
      assertContains(it, getGradleOptions())
      assertContains(it, getRootProjectTasks(), getRootProjectTasks(":"), !getRootProjectTasks(":module:"))
      assertContains(it, getCommonTasks(), getCommonTasks(":"), getCommonTasks(":module:"))
      assertContains(it, "taskP", ":taskP", !":module:taskP")
      assertContains(it, "taskM", !":taskM", ":module:taskM")
      assertContains(it, !"taskC", !":taskC", !":module:taskC")
      assertContains(it, !"taskCM", !":taskCM", !":module:taskCM")
    }
    withVariantsFor("", "project.module") {
      assertContains(it, getGradleOptions())
      assertContains(it, !getRootProjectTasks(), !getRootProjectTasks(":"), !getRootProjectTasks(":module:"))
      assertContains(it, getCommonTasks(), getCommonTasks(":"), !getCommonTasks(":module:"))
      assertContains(it, !"taskP", !":taskP", !":module:taskP")
      assertContains(it, "taskM", ":taskM", !":module:taskM")
      assertContains(it, !"taskC", !":taskC", !":module:taskC")
      assertContains(it, !"taskCM", !":taskCM", !":module:taskCM")
    }
    withVariantsFor("", "composite") {
      assertContains(it, getGradleOptions())
      assertContains(it, getRootProjectTasks(), getRootProjectTasks(":"), !getRootProjectTasks(":module:"))
      assertContains(it, getCommonTasks(), getCommonTasks(":"), getCommonTasks(":module:"))
      assertContains(it, !"taskP", !":taskP", !":module:taskP")
      assertContains(it, !"taskM", !":taskM", !":module:taskM")
      assertContains(it, "taskC", ":taskC", !":module:taskC")
      assertContains(it, "taskCM", !":taskCM", ":module:taskCM")
    }
    withVariantsFor("", "composite.module") {
      assertContains(it, getGradleOptions())
      assertContains(it, !getRootProjectTasks(), !getRootProjectTasks(":"), !getRootProjectTasks(":module:"))
      assertContains(it, getCommonTasks(), getCommonTasks(":"), !getCommonTasks(":module:"))
      assertContains(it, !"taskP", !":taskP", !":module:taskP")
      assertContains(it, !"taskM", !":taskM", !":module:taskM")
      assertContains(it, !"taskC", !":taskC", !":module:taskC")
      assertContains(it, "taskCM", ":taskCM", !":module:taskCM")
    }
  }
}