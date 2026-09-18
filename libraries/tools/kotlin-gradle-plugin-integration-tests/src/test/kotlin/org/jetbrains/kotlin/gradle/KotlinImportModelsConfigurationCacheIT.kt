/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.importmodels.proto.Result
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * POC: the import models are outputs of the `generateKotlinImportModels` task, so on a configuration cache hit
 * the project is not configured and the models are not recomputed - the task is `UP-TO-DATE` and the model builder
 * only reads the `.pb` files.
 *
 * The task is run as a separate Gradle invocation, not via `BuildActionExecuter.forTasks`: a plain task invocation is
 * cached by the configuration cache with and without Isolated Projects, while a model-building invocation is cached
 * only with Isolated Projects.
 */
@JvmGradlePluginTests
class KotlinImportModelsConfigurationCacheIT : KGPBaseTest() {
    override val defaultBuildOptions
        get() = super.defaultBuildOptions.copy(
            configurationCache = BuildOptions.ConfigurationCacheValue.ENABLED,
            isolatedProjects = BuildOptions.IsolatedProjectsMode.ENABLED,
        )

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_9_0)
    fun `import models are not recomputed on a configuration cache hit`(gradleVersion: GradleVersion) {
        project("simpleProject", gradleVersion) {
            // configures the project, computes the models, stores the configuration cache
            build(GENERATE_TASK) {
                assertConfigurationCacheStored()
                assertTasksExecuted(":$GENERATE_TASK")
            }
            val models = sync()
            assertTrue(models.all { it.hasModel() }, "Models must not contain errors")

            // configuration cache hit: nothing is configured or recomputed, the task is UP-TO-DATE
            build(GENERATE_TASK) {
                assertConfigurationCacheReused()
                assertTasksUpToDate(":$GENERATE_TASK")
            }
            assertEquals(models, sync(), "Repeated sync must return identical models")
        }
    }

    // All models of the root project as an IDE sync would read them
    private fun TestProject.sync(): List<Result> = with(runBuildAction(KotlinImportModelsBuildAction()).toModels()) {
        buildList {
            addAll(listOf(this@with.base, this@with.project))
            addAll(this@with.compilationUnits)
            addAll(this@with.compilerArguments)
            addAll(this@with.dependencies)
        }
    }
}
