/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Plugin for registering the `manageTestData` and `updateTestData` tasks in a module.
 *
 * Apply this plugin to modules that have managed test data.
 *
 * ## Tasks
 *
 * - **`manageTestData`** ([TestDataManagerModuleTask]) — supports both `--mode=check` (default)
 *   and `--mode=update`. Options accepted via `--option` CLI flags.
 * - **`updateTestData`** ([UpdateTestDataModuleTask]) — always runs in `update` mode. Options
 *   accepted only via `-P` Gradle properties so the configuration cache stays valid when
 *   option values change between runs.
 *
 * ## Usage
 *
 * ```bash
 * # Check mode (CLI options)
 * ./gradlew :analysis:analysis-api-fir:manageTestData --test-data-path=path/to/file.kt
 *
 * # Update mode via the dedicated task (CC-friendly, -P options)
 * ./gradlew :analysis:analysis-api-fir:updateTestData \
 *     -Porg.jetbrains.kotlin.testDataManager.options.testDataPath=path/to/file.kt
 *
 * # Or across all modules with the plugin (Gradle task-name matching)
 * ./gradlew updateTestData -Porg.jetbrains.kotlin.testDataManager.options.testClassPattern=.*Fir.*
 * ```
 *
 * When `manageTestData` is run via orchestration from `manageTestDataGlobally`, it pulls
 * configuration from the shared [TestDataManagerConfiguration] extension. `updateTestData`
 * has no orchestrator — each module reads its own `-P` properties independently.
 *
 * ## Ordering
 *
 * Both tasks inherit `mustRunAfter` from the module's `test` task, ensuring proper execution
 * order when running across modules (e.g., golden modules first).
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

    wireFromTestTask(manageTestDataTaskName)
}

/**
 * Wires a test-data manager-style [JavaExec] task to mirror the module's regular `test` task
 * so tests run the same way under the manager as they do normally.
 *
 * Shared by both [TestDataManagerModuleTask] and [UpdateTestDataModuleTask] registrations.
 *
 * @param peerTaskName the name of the manager task being configured. Used to rewrite
 *   `mustRunAfter` constraints so that, e.g., `:moduleA:test → :moduleB:test` becomes
 *   `:moduleA:peerTaskName → :moduleB:peerTaskName` — preserving cross-module ordering
 *   between manager-task instances.
 */
private fun JavaExec.wireFromTestTask(peerTaskName: String) {
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
    jvmArgumentProviders += testTask.jvmArgumentProviders
    javaLauncher = testTask.javaLauncher

    /**
     * This disables `KotlinSecurityManager` in the Test Data Manager.
     * Currently, the policy file is only generated in 'test' tasks (see `test-inputs-check.gradle.kts`),
     * so the policy will be non-existent until the corresponding test task runs. However, the following
     * properties are still carried on from the 'test' task – unless they're filtered, the Test Data
     * Manager process will crash:
     *
     * ```
     * -Djava.security.manager=org.jetbrains.kotlin.security.KotlinSecurityManager
     * -Djava.security.policy=some/subproject/path/build/permissions-for-test.policy
     * ```
     *
     * Also see KT-84278.
     */
    systemProperties = testTask.systemProperties.filterKeys { !it.startsWith("java.security.") }

    // Forward idea.active to enable IDE integration in TestDataManagerRunner
    if (project.providers.systemProperty("idea.active").isPresent) {
        systemProperty("idea.active", "true")
    }

    // Pass project name for unique test IDs when running multiple modules in parallel
    systemProperty(TestDataManagerOption.PROJECT_NAME, project.path)
}

tasks.register<UpdateTestDataModuleTask>(updateTestDataTaskName) {
    markAsIdeaTestTask()

    wireFromTestTask(updateTestDataTaskName)
}
