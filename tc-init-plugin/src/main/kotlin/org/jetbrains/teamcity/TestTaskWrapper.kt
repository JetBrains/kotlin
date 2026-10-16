package org.jetbrains.teamcity

import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.process.JavaForkOptions

class TestTaskWrapper(
    private val task: AbstractTestTask,
    private val buildDirectoriesResolver: BuildDirectoriesResolver,
    private val testNameFormat: String?,
    private val ignoredSuiteFormat: String?
) {
    fun configure() {
        // Setup test filter for dependency-based test runs / split tests
        DependencyBasedTestRun.configure(task)

        // Add TeamCity test listener
        val testListener = TeamcityTestListener(
            task.path,
            buildDirectoriesResolver,
            testNameFormat,
            ignoredSuiteFormat,
            task.logger
        )
        task.addTestListener(testListener)
        task.addTestOutputListener(testListener)

        // Add test JVM arguments if configured
        val testCoverageJvmArgs = System.getenv(Constants.GRADLE_TEST_COVERAGE_JVM_ARGS_ENV_VAR)
        if (!testCoverageJvmArgs.isNullOrEmpty() && task is JavaForkOptions) {
            val jvmArgsList = testCoverageJvmArgs.split(" ").filter { it.isNotBlank() }
            if (System.getenv(Constants.TEST_TASK_JVM_ARG_PROVIDER_DISABLED_ENV_VAR)?.toBoolean() == true) {
                task.jvmArgs(jvmArgsList)
            } else {
                task.jvmArgumentProviders.add(TestTaskJvmArgumentsProvider(jvmArgsList))
            }
        }
    }
}
