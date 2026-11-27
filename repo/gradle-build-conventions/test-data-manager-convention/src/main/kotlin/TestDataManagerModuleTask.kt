/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Prints what the `test-data-manager` wrapper needs to know of the module: its project path, its directory and the test
 * data directories it declares, as one line after [OUTPUT_PREFIX], separated by tabs. Run from the root as
 * `testDataManagerModule`, it prints every module with the `test-data-manager` plugin.
 *
 * The wrapper runs a module only for the paths in or around its declared test data, so the declarations must be complete:
 * the module must apply `test-inputs-check`, which fails the tests on any other test data they read.
 */
@DisableCachingByDefault(because = "Prints to the console")
abstract class TestDataManagerModuleTask : DefaultTask() {
    @get:Input
    abstract val projectPath: Property<String>

    @get:Input
    abstract val projectDirectory: Property<String>

    /** The absolute paths of the declared test data directories, `projectTests { testData(...) }`. */
    @get:Input
    abstract val testData: ListProperty<String>

    @get:Input
    abstract val testInputsChecked: Property<Boolean>

    @TaskAction
    fun print() {
        if (!testInputsChecked.get()) {
            throw GradleException(
                "${projectPath.get()} applies `test-data-manager` without `test-inputs-check`: the test data it declares, " +
                        "which the test data manager selects the module by, must be checked. Apply id(\"test-inputs-check\").",
            )
        }

        val fields = listOf(projectPath.get(), projectDirectory.get()) + testData.get()
        logger.quiet(OUTPUT_PREFIX + fields.joinToString("\t"))
    }

    companion object {
        const val OUTPUT_PREFIX = "Test data manager module: "
    }
}
