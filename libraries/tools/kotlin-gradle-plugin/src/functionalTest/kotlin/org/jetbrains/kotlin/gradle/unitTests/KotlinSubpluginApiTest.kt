/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.ParcelizeExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.CreateCompilerArgumentsContext
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType.PluginClasspath
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.ArgumentType.Primitive
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.testing.normalizeSnapshotJarName
import org.jetbrains.kotlin.gradle.util.androidLibrary
import org.jetbrains.kotlin.gradle.util.applyKotlinAndroidPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KotlinSubpluginApiTest {
    @Test
    fun `parcelize - is applicable to android only - old target`() {
        assertParcelizeApplied(buildParcelizeProject(), implicitlyAddParcelableSupertype = false)
    }

    @Test
    fun `parcelize - implicitlyAddParcelableSupertype via extension - applies to android and metadata compiled for android`() {
        val project = buildParcelizeProject {
            extensions.getByType(ParcelizeExtension::class.java).implicitlyAddParcelableSupertype.set(true)
        }
        assertParcelizeApplied(project, implicitlyAddParcelableSupertype = true)
    }

    @Test
    fun `parcelize - implicitlyAddParcelableSupertype via extension - exposes the runtime as api - multiplatform`() {
        assertEquals(false, buildParcelizeProject().hasParcelizeRuntime("commonMainApi"))
        val project = buildParcelizeProject {
            extensions.getByType(ParcelizeExtension::class.java).implicitlyAddParcelableSupertype.set(true)
        }
        assertEquals(true, project.hasParcelizeRuntime("commonMainApi"))
    }

    @Test
    fun `parcelize - implicitlyAddParcelableSupertype via extension - exposes the runtime as api - android`() {
        assertEquals(false, buildAndroidParcelizeProject().hasParcelizeRuntime("api"))
        val project = buildAndroidParcelizeProject {
            extensions.getByType(ParcelizeExtension::class.java).implicitlyAddParcelableSupertype.set(true)
        }
        assertEquals(true, project.hasParcelizeRuntime("api"))
    }

    private fun Project.hasParcelizeRuntime(configurationName: String): Boolean =
        configurations.getByName(configurationName).dependencies.any {
            it.group == "org.jetbrains.kotlin" && it.name == "kotlin-parcelize-runtime"
        }

    private fun buildAndroidParcelizeProject(configure: Project.() -> Unit = {}): Project {
        val project = buildProject {
            configureRepositoriesForTests()
            androidLibrary {}
            applyKotlinAndroidPlugin()
            plugins.apply("kotlin-parcelize")
            configure()
        }
        project.evaluate()
        return project
    }

    private fun buildParcelizeProject(configure: Project.() -> Unit = {}): Project {
        val project = buildProjectWithMPP {
            configureRepositoriesForTests()
            androidLibrary {}
            plugins.apply("kotlin-parcelize")
            kotlin {
                @Suppress("DEPRECATION")
                androidTarget()
                jvm()
                linuxX64()
                linuxArm64()
                js()

                wasmJs()
                @OptIn(ExperimentalWasmDsl::class)
                wasmWasi()
            }
            configure()
        }
        project.evaluate()
        return project
    }

    private fun assertParcelizeApplied(project: Project, implicitlyAddParcelableSupertype: Boolean) {
        val resolutionContext = CreateCompilerArgumentsContext(
            includeArgumentTypes = setOf(PluginClasspath, Primitive)
        )
        val parcelizeJar = "kotlin-parcelize-compiler-${project.getKotlinPluginVersion()}.jar"
        val implicitSupertypeOption = "plugin:org.jetbrains.kotlin.parcelize:implicitlyAddParcelableSupertype=true"

        val compilations = project.multiplatformExtension.targets.flatMap { it.compilations }
        val metadataCompilations = compilations.filterIsInstance<KotlinMetadataCompilation<*>>().map { it.name }.toSet()
        assertTrue(
            metadataCompilations.containsAll(setOf("commonMain", "webMain", "nativeMain")),
            "The project must have metadata compilations for a source set that is compiled for Android and for some that are not, " +
                    "got: $metadataCompilations",
        )

        compilations.forEach { compilation ->
            // Only `commonMain` is compiled for Android. `webMain`, `nativeMain`, and `linuxMain` are not.
            // The IDE takes the plugin arguments of shared source sets from the `main` metadata compilation.
            val expectPlugin = compilation.target.name == "android" ||
                    (implicitlyAddParcelableSupertype && compilation is KotlinMetadataCompilation<*> &&
                            compilation.name in setOf("commonMain", KotlinCompilation.MAIN_COMPILATION_NAME))
            val compileTask = compilation.compileTaskProvider.get() as KotlinCompilerArgumentsProducer
            val args = compileTask.createCompilerArguments(resolutionContext) as CommonCompilerArguments
            assertEquals(
                expectPlugin,
                args.pluginClasspaths.orEmpty().any { File(it).name.normalizeSnapshotJarName() == parcelizeJar },
                "Presence of the kotlin-parcelize plugin in the arguments of $compileTask",
            )
            assertEquals(
                expectPlugin && implicitlyAddParcelableSupertype,
                implicitSupertypeOption in args.pluginOptions.orEmpty(),
                "Presence of '$implicitSupertypeOption' in the arguments of $compileTask",
            )
        }

        val commonMainLanguageSettings =
            project.multiplatformExtension.sourceSets.getByName("commonMain").languageSettings as DefaultLanguageSettingsBuilder
        assertEquals(
            implicitlyAddParcelableSupertype,
            implicitSupertypeOption in commonMainLanguageSettings.compilerPluginArguments.orEmpty(),
            "Presence of '$implicitSupertypeOption' in the IDE plugin arguments of commonMain",
        )
    }
}
