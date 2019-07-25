import com.intellij.openapi.externalSystem.model.ExternalSystemException
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.testFramework.RunAll
import com.intellij.util.ThrowableRunnable
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.jetbrains.plugins.gradle.GradleManager
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsoleManager
import org.jetbrains.plugins.gradle.importing.GradleBuildScriptBuilderEx
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase
import org.jetbrains.plugins.gradle.importing.withMavenCentral
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.Assert.*
import org.junit.Test

open class GradleJavaTestEventsIntegrationTest: GradleImportingTestCase() {

  @Test
  fun test() {
    createProjectSubFile("src/main/java/my/pack/AClass.java",
                         "package my.pack;\n" +
                         "public class AClass {\n" +
                         "  public int method() { return 42; }" +
                         "}");

    createProjectSubFile("src/test/java/my/pack/AClassTest.java",
                         "package my.pack;\n" +
                         "import org.junit.Test;" +
                         "import static org.junit.Assert.*;" +
                         "public class AClassTest {\n" +
                         "  @Test\n" +
                         "  public void testSuccess() {\n" +
                         "    assertEquals(42, new AClass().method());\n" +
                         "  }\n" +
                         "  @Test\n" +
                         "  public void testFail() {\n" +
                         "    fail(\"failure message\");\n" +
                         "  }\n" +
                         "}");

    importProject(
      GradleBuildScriptBuilderEx()
        .withMavenCentral()
        .applyPlugin("'java'")
        .addPostfix("dependencies {",
                    "  testCompile 'junit:junit:4.12'",
                    "}")
        .generate()
    )

    RunAll().append(
      ThrowableRunnable<Throwable> { `call test task produces test events`() },
      ThrowableRunnable<Throwable> { `call build task does not produce test events`() }
    ).run()
  }

  private fun `call test task produces test events`() {
    val taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, myProject)
    val eventLog = mutableListOf<String>()
    val testListener = object : ExternalSystemTaskNotificationListenerAdapter() {
      override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        eventLog.add(text.trim('\r', '\n', ' '))
      }
    };

    val settings = GradleManager().executionSettingsProvider.`fun`(Pair.create<Project, String>(myProject, projectPath))
    settings.putUserData(GradleConstants.RUN_TASK_AS_TEST, true);

    assertThatThrownBy {
      GradleTaskManager().executeTasks(taskId,
                                       listOf(":test"),
                                       projectPath,
                                       settings,
                                       null,
                                       testListener);
    }
      .hasMessageContaining("There were failing tests")

    assertThat(eventLog)
      .contains(
        "<descriptor name='testFail' className='my.pack.AClassTest' /><ijLogEol/>",
        "<descriptor name='testSuccess' className='my.pack.AClassTest' /><ijLogEol/>"
      )
  }

  private fun `call build task does not produce test events`() {
    val taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, myProject)
    val eventLog = mutableListOf<String>()
    val testListener = object : ExternalSystemTaskNotificationListenerAdapter() {
      override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        eventLog.add(text.trim('\r', '\n', ' '))
      }
    };

    val settings = GradleManager().executionSettingsProvider.`fun`(Pair.create<Project, String>(myProject, projectPath))

    assertThatThrownBy {
      GradleTaskManager().executeTasks(taskId,
                                       listOf("clean", "build"),
                                       projectPath,
                                       settings,
                                       null,
                                       testListener);
    }
      .hasMessageContaining("There were failing tests")
    assertThat(eventLog).noneMatch { it.contains("<ijLogEol/>") }
  }

}
