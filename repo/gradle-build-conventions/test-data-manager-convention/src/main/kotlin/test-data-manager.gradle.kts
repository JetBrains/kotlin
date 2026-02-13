/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Plugin for registering the `manageTestData` task in a module.
 *
 * Apply this plugin to modules that have managed test data.
 *
 * ## Usage
 *
 * The task can be run in two ways:
 *
 * 1. **Directly** on a single module:
 *    ```bash
 *    ./gradlew :analysis:analysis-api-fir:manageTestData --mode=update
 *    ```
 *
 * 2. **Via orchestration** from the root `manageTestDataGlobally` task:
 *    ```bash
 *    ./gradlew manageTestDataGlobally --mode=update
 *    ```
 *
 * When run via orchestration, the task pulls configuration from the shared
 * [TestDataManagerConfiguration] extension. When run directly, it uses
 * its own `@Option` values from CLI.
 *
 * ## Ordering
 *
 * The task inherits `mustRunAfter` from the module's `test` task, ensuring
 * proper execution order when running globally (e.g., golden modules first).
 */

tasks.register<TestDataManagerModuleTask>(manageTestDataTaskName) {
    markAsIdeaTestTask()

    // Wire providers from shared config
    // Note: the config might have values only in the case running the task by the global one
    val rootConfig = rootProject.extensions.getByType<TestDataManagerConfiguration>()
    mode.convention(rootConfig.mode.orElse(TestDataManagerMode.DEFAULT))
    testDataPath.convention(rootConfig.testDataPath)
    testClassPattern.convention(rootConfig.testClassPattern)
    goldenOnly.convention(rootConfig.goldenOnly)
    incremental.convention(rootConfig.incremental)

    // Capture test task configuration eagerly during configuration (configuration-cache compatible)
    // Note: taskProvider.map creates a task dependency, so we capture the value directly
    val testTask = tasks.named<Test>("test").get()

    // Copy all test task dependencies - more robust than manually listing them
    dependsOn(testTask.dependsOn)

    // Inherit ordering from test task, but convert :test references to :manageTestData
    // This ensures proper ordering when running manageTestDataGlobally
    val testMustRunAfter = testTask.mustRunAfter.getDependencies(testTask)
    testMustRunAfter.forEach { dependency ->
        val taskPath = dependency.path
        if (taskPath.endsWith(":test")) {
            // Convert :module:test -> :module:manageTestData
            val manageTestDataPath = "${dependency.project.path}:$manageTestDataTaskName"
            mustRunAfter(manageTestDataPath)
        }
    }

    // Use testTask.classpath to include both compiled test classes AND dependencies
    classpath = testTask.classpath
    workingDir = testTask.workingDir
    jvmArgs = testTask.jvmArgs
    jvmArgumentProviders += testTask.jvmArgumentProviders
    systemProperties = testTask.systemProperties

    // Forward idea.active to enable IDE integration in TestDataManagerRunner
    if (project.providers.systemProperty("idea.active").isPresent) {
        systemProperty("idea.active", "true")
    }

    // Pass project name for unique test IDs when running multiple modules in parallel
    systemProperty("$testDataManagerOptionsPrefix.projectName", project.path)
}
