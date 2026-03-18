/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Plugin for registering the root `manageTestDataGlobally` task.
 *
 * Apply this plugin to the root project to get an aggregating task that
 * orchestrates test data management across all modules that have the
 * `test-data-manager` plugin applied.
 *
 * ## Architecture
 *
 * This plugin creates a shared [TestDataManagerConfiguration] extension that holds
 * configuration values. The root task publishes its `@Option` values to this extension,
 * and module tasks pull from it when running in orchestrated mode.
 *
 * Execution order is determined by each module's `mustRunAfter` inherited from
 * its `test` task, ensuring golden modules run before dependent ones.
 *
 * ## Usage
 *
 * ```kotlin
 * // In root build.gradle.kts
 * plugins {
 *     id("test-data-manager-root")
 * }
 * ```
 *
 * Then run:
 * ```bash
 * # Global orchestration across all modules
 * ./gradlew manageTestDataGlobally                            # Check all test data (default)
 * ./gradlew manageTestDataGlobally --mode=update              # Update all test data
 * ./gradlew manageTestDataGlobally --test-data-path=path/to/testData  # Filter by path
 * ./gradlew manageTestDataGlobally --test-class-pattern=.*Fir.*       # Filter by test class
 * ./gradlew help --task manageTestDataGlobally                # Show available options
 *
 * # Direct invocation on a single module (no root plugin needed)
 * ./gradlew :analysis:analysis-api-fir:manageTestData --mode=update
 * ```
 *
 * To filter modules in global mode, use project property:
 * ```bash
 * ./gradlew manageTestDataGlobally -Porg.jetbrains.kotlin.testDataManager.options.module=:analysis:analysis-api-fir
 * ```
 */

// Create shared configuration extension
val config = extensions.create<TestDataManagerConfiguration>(TestDataManagerConfiguration.NAME)

tasks.register<TestDataManagerGlobalTask>(manageTestDataGloballyTaskName) {
    markAsIdeaTestTask()

    config.mode.set(mode)
    config.testDataPath.set(testDataPath)
    config.testClassPattern.set(testClassPattern)
    config.goldenOnly.set(goldenOnly)
    config.incremental.set(incremental)
}

// Configure task dependencies after all projects are evaluated
gradle.projectsEvaluated {
    val rootTaskProvider = tasks.named<TestDataManagerGlobalTask>(manageTestDataGloballyTaskName)

    // Discover modules with test-data-manager plugin
    val allModules = project.subprojects.filter {
        it.plugins.hasPlugin("test-data-manager")
    }

    // Support -Porg.jetbrains.kotlin.testDataManager.options.module filter
    val requestedModules = project.findProperty("$testDataManagerOptionsPrefix.module")
        ?.toString()
        ?.split(",")
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        ?.toSet()

    val modulesToRun = if (requestedModules != null) {
        allModules.filter { it.path in requestedModules }.also { filtered ->
            val notFound = requestedModules - filtered.map { it.path }.toSet()
            if (notFound.isNotEmpty()) {
                logger.warn("Requested modules not found or don't have test-data-manager plugin: $notFound")
            }
        }
    } else {
        allModules
    }

    if (modulesToRun.isEmpty()) {
        rootTaskProvider.configure {
            doFirst {
                logger.warn("No modules with test-data-manager plugin found")
            }
        }

        return@projectsEvaluated
    }

    // Set up dependsOn all module tasks
    // Ordering is handled by mustRunAfter inherited from each module's test task
    val modulePaths = modulesToRun.map { it.path }.sorted()

    rootTaskProvider.configure {
        modulePaths.forEach { modulePath ->
            dependsOn("$modulePath:$manageTestDataTaskName")
        }

        doFirst {
            logger.lifecycle("Running $manageTestDataGloballyTaskName for ${modulePaths.size} module(s):")
            modulePaths.forEach { path ->
                logger.lifecycle("  $path")
            }
        }
    }
}