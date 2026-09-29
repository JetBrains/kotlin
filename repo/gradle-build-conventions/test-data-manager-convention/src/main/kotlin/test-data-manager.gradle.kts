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
 * [TestDataManagerExtension.mustRunAfterProjects] orders the tasks, as well as `test`, across modules
 * (e.g., golden modules first):
 *
 * ```kotlin
 * testDataManager {
 *     mustRunAfterProjects.add(":analysis:analysis-api-fir")
 * }
 * ```
 */

val testDataManager = extensions.create<TestDataManagerExtension>("testDataManager")

tasks.named { it == "test" }.configureEach {
    mustRunAfter(testDataManager.mustRunAfterProjects.map { projectPaths -> projectPaths.map { "$it:test" } })
}

tasks.register<CheckTestDataModuleTask>(checkTestDataTaskName) {
    wireOptions(testDataManager)
}

tasks.register<UpdateTestDataModuleTask>(updateTestDataTaskName) {
    wireOptions(testDataManager)
}

/**
 * Wires a test-data manager-style [JavaExec] task to mirror the module's regular `test` task
 * so tests run the same way under the manager as they do normally.
 *
 * Shared by both [CheckTestDataModuleTask] and [UpdateTestDataModuleTask] registrations.
 */
private fun AbstractTestDataModuleTask.wireOptions(extension: TestDataManagerExtension) {
    /**
     * Wires each option's convention from its `-P` Gradle property. The providers are resolved lazily at
     * execution time, so they are not configuration-cache inputs — see [AbstractTestDataModuleTask].
     */
    testDataPath.convention(project.providers.gradleProperty(TestDataManagerOption.TEST_DATA_PATH))
    testClassPattern.convention(project.providers.gradleProperty(TestDataManagerOption.TEST_CLASS_PATTERN))
    goldenOnly.convention(project.providers.gradleProperty(TestDataManagerOption.GOLDEN_ONLY).map { it.toBoolean() })
    incremental.convention(project.providers.gradleProperty(TestDataManagerOption.INCREMENTAL).map { it.toBoolean() })

    /**
     * The `Test` task configured exactly like the module's `test`, but never executed (see `createGeneralTestTask`).
     *
     * All options below are mapped lazily from it. With the configuration cache (on by default in this repository),
     * the mapped providers are evaluated once, when the cache entry is stored, i.e., after all configuration
     * of the carrier has been applied, so the `Test` instance is never read while this task executes.
     * Without the configuration cache, they are evaluated when first queried, right before execution.
     */
    val carrier = tasks.named<Test>(testDataManagerWarmupTaskName)

    // Runs everything the carrier depends on: compilation of the test classpath and all its other inputs.
    // The carrier itself is skipped by its `onlyIf`.
    dependsOn(carrier)

    // Orders same-named tasks across modules, e.g., `:moduleB:checkTestData` after `:moduleA:checkTestData`.
    // Resolved as plain task paths when the task graph is built.
    val peerTaskName = name
    mustRunAfter(extension.mustRunAfterProjects.map { projectPaths -> projectPaths.map { "$it:$peerTaskName" } })

    javaLauncher.set(carrier.flatMap { it.javaLauncher })
    workingDirectory.set(carrier.flatMap { it.workingDirectory })

    // Includes both compiled test classes AND dependencies.
    // `Test.classpath` is a plain `FileCollection` that may be replaced later, so a provider of it is used.
    classpath(carrier.map { it.classpath })

    // `JavaExec` parses `-ea`, `-Xms`, `-Xmx` and `-D` from `jvmArguments` back into the corresponding options
    // at execution time, so this is equivalent to copying them one by one, but lazy.
    jvmArguments.addAll(carrier.map { test ->
        buildList {
            addAll(test.jvmArgs)
            if (test.enableAssertions) add("-ea")
            test.minHeapSize?.let { add("-Xms$it") }
            test.maxHeapSize?.let { add("-Xmx$it") }

            /**
             * Filter out system properties used by `test-inputs-check`.
             * Otherwise, the task would crash with either missing security policy or `declared-inputs-for-test.txt` file.
             *
             * Also see KT-84278.
             */
            test.systemProperties
                .filterKeys { !it.startsWith("java.security.") && !it.startsWith("test.instrumenter.") }
                .forEach { (key, value) -> add(if (value == null) "-D$key" else "-D$key=$value") }
        }
    })

    inheritedEnvironment.set(carrier.map { test -> test.environment.mapValues { it.value.toString() } })

    /**
     * Filter out JVM argument provider used by `test-inputs-check`
     */
    inheritedJvmArgumentProviders.set(carrier.map { test ->
        test.jvmArgumentProviders.filter { it !is JfrArgumentProvider }
    })

    // IDE integration: mark the task the same way as `Test` so IDEA's test runner picks it up
    // and forwards `idea.active` to enable IDE integration in `TestDataManagerRunner`.
    if (project.providers.systemProperty("idea.active").isPresent) {
        extra["idea.internal.test"] = true
        systemProperty("idea.active", "true")
    }

    // Pass project name for unique test IDs when running multiple modules in parallel
    systemProperty(TestDataManagerOption.PROJECT_NAME, project.path)
}
