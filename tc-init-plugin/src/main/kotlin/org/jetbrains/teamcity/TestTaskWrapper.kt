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
        // Enhance Gradle daemon classpath if requested
        GradleDaemonClasspathEnhancer.enhanceWithTestClasspath(task)

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
        if (!testCoverageJvmArgs.isNullOrEmpty()) {
            val jvmArgsList = testCoverageJvmArgs.split(" ").filter { it.isNotBlank() }
            if (task is JavaForkOptions) {
                if (System.getenv(Constants.TEST_TASK_JVM_ARG_PROVIDER_DISABLED_ENV_VAR)?.toBoolean() == true) {
                    task.jvmArgs(jvmArgsList)
                } else {
                    task.jvmArgumentProviders.add(TestTaskJvmArgumentsProvider(jvmArgsList))
                }
            } else {
                try {
                    if (System.getenv(Constants.TEST_TASK_JVM_ARG_PROVIDER_DISABLED_ENV_VAR)?.toBoolean() == true) {
                        val method = task.javaClass.getMethod("jvmArgs", Iterable::class.java)
                        method.invoke(task, jvmArgsList)
                    } else {
                        val method = task.javaClass.getMethod("getJvmArgumentProviders")
                        @Suppress("UNCHECKED_CAST")
                        val providers = method.invoke(task) as MutableList<Any>
                        providers.add(TestTaskJvmArgumentsProvider(jvmArgsList))
                    }
                } catch (ignored: Throwable) {}
            }
        }
    }
}
