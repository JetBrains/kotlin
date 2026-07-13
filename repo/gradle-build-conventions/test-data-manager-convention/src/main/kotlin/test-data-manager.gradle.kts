/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Plugin for registering the `checkTestData` and `updateTestData` tasks in a module.
 *
 * Apply this plugin to modules that have managed test data.
 *
 * ## Tasks
 *
 * - **`checkTestData`** ([CheckTestDataModuleTask]) — runs tests and fails on mismatches without
 *   modifying anything.
 * - **`updateTestData`** ([UpdateTestDataModuleTask]) — runs tests and updates files on mismatches.
 *
 * Both accept their options only via `-P` Gradle properties (not `--option` CLI flags) — see
 * [AbstractTestDataModuleTask] for the rationale and the full option list.
 *
 * ## Usage
 *
 * ```bash
 * # Check mode
 * ./gradlew :analysis:analysis-api-fir:checkTestData \
 *     -Porg.jetbrains.kotlin.testDataManager.options.testDataPath=path/to/file.kt
 *
 * # Update mode
 * ./gradlew :analysis:analysis-api-fir:updateTestData \
 *     -Porg.jetbrains.kotlin.testDataManager.options.testDataPath=path/to/file.kt
 *
 * # Or across all modules with the plugin (Gradle task-name matching)
 * ./gradlew updateTestData -Porg.jetbrains.kotlin.testDataManager.options.testClassPattern=.*Fir.*
 * ```
 *
 * There is no global orchestrator task — each module reads its own `-P` properties independently.
 *
 * ## Ordering
 *
 * Both tasks inherit `mustRunAfter` from the module's `test` task, ensuring proper execution
 * order when running across modules (e.g., golden modules first).
 */

tasks.register<CheckTestDataModuleTask>(checkTestDataTaskName) {
    wireOptions(checkTestDataTaskName)
}

tasks.register<UpdateTestDataModuleTask>(updateTestDataTaskName) {
    wireOptions(updateTestDataTaskName)
}

/**
 * Wires a test-data manager-style [JavaExec] task to mirror the module's regular `test` task
 * so tests run the same way under the manager as they do normally.
 *
 * Shared by both [CheckTestDataModuleTask] and [UpdateTestDataModuleTask] registrations.
 *
 * @param peerTaskName the name of the manager task being configured. Used to rewrite
 *   `mustRunAfter` constraints so that, e.g., `:moduleA:test → :moduleB:test` becomes
 *   `:moduleA:peerTaskName → :moduleB:peerTaskName` — preserving cross-module ordering
 *   between manager-task instances.
 */
private fun AbstractTestDataModuleTask.wireOptions(peerTaskName: String) {
    /**
     * Wires each option's convention from its `-P` Gradle property. The providers are resolved lazily at
     * execution time, so they are not configuration-cache inputs — see [AbstractTestDataModuleTask].
     */
    testDataPath.convention(project.providers.gradleProperty(TestDataManagerOption.TEST_DATA_PATH))
    testClassPattern.convention(project.providers.gradleProperty(TestDataManagerOption.TEST_CLASS_PATTERN))
    goldenOnly.convention(project.providers.gradleProperty(TestDataManagerOption.GOLDEN_ONLY).map { it.toBoolean() })
    incremental.convention(project.providers.gradleProperty(TestDataManagerOption.INCREMENTAL).map { it.toBoolean() })

    // Capture test task configuration eagerly during configuration (configuration-cache compatible)
    // Note: taskProvider.map creates a task dependency, so we capture the value directly
    val testTask = tasks.named<Test>("test").get()

    // Copy all test task dependencies and inputs, so tests run the same way in the manager as they run normally
    dependsOn(testTask.dependsOn)
    dependsOn(testTask.inputs)

    // Inherit ordering from a test task but convert `:test` references to `:peerTaskName`.
    // This ensures proper ordering when multiple modules' manager tasks run together (e.g., golden modules first).
    val testMustRunAfter = testTask.mustRunAfter.getDependencies(testTask)
    testMustRunAfter.forEach { dependency ->
        if (dependency.path.endsWith(":test")) {
            // Convert :module:test -> :module:peerTaskName
            mustRunAfter("${dependency.project.path}:$peerTaskName")
        }
    }

    // Use testTask.classpath to include both compiled test classes AND dependencies
    classpath = testTask.classpath
    workingDir = testTask.workingDir
    environment = testTask.environment
    jvmArgs = testTask.jvmArgs
    enableAssertions = testTask.enableAssertions
    minHeapSize = testTask.minHeapSize
    maxHeapSize = testTask.maxHeapSize
    javaLauncher = testTask.javaLauncher

    /**
     * Filter out system properties used by `test-inputs-check` and `test-inputs-check-v2`.
     * Otherwise, the task would crash with either missing security policy or `declared-inputs-for-test.txt` file.
     *
     * Also see KT-84278.
     */
    systemProperties = testTask.systemProperties.filterKeys {
        !it.startsWith("java.security.") && !it.startsWith("test.instrumenter.")
    }

    /**
     * Filter out JVM argument provider used by `test-inputs-check-v2`
     */
    jvmArgumentProviders += testTask.jvmArgumentProviders
        .filter { it !is JfrArgumentProvider }

    // IDE integration: mark the task the same way as `Test` so IDEA's test runner picks it up
    // and forwards `idea.active` to enable IDE integration in `TestDataManagerRunner`.
    if (project.providers.systemProperty("idea.active").isPresent) {
        extra["idea.internal.test"] = true
        systemProperty("idea.active", "true")
    }

    // Pass project name for unique test IDs when running multiple modules in parallel
    systemProperty(TestDataManagerOption.PROJECT_NAME, project.path)
}
