/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.readLines
import kotlin.test.assertEquals

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@NativeGradlePluginTests
class ParcelizeNativeDependencyResolutionIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(enableLegacyAgpDsl = false)

    // https://github.com/JetBrains/kotlin/pull/7488#issuecomment-6081809347
    @GradleAndroidTest
    @AndroidTestVersions(minVersion = TestVersions.AGP.AGP_811)
    fun parcelizeRuntimeResolvesForNativeTargets(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion,
            androidVersion,
            jdkVersion,
            additionalPlugins = { kotlin("plugin.parcelize") },
        ) {
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    iosX64()
                    @Suppress("DEPRECATION")
                    androidNativeArm32()
                    @Suppress("DEPRECATION")
                    androidNativeArm64()
                    @Suppress("DEPRECATION")
                    androidNativeX86()
                    @Suppress("DEPRECATION")
                    androidNativeX64()
                }

                val runtimeArtifacts = project.files(kotlinMultiplatform.targets.filterIsInstance<KotlinNativeTarget>().map { target ->
                    val compilation = target.compilations.getByName("main")
                    val configuration = project.configurations.getByName(compilation.compileDependencyConfigurationName)
                    configuration.incoming.artifactView { view ->
                        view.componentFilter { component ->
                            component is ModuleComponentIdentifier && component.module.startsWith("kotlin-parcelize-runtime")
                        }
                    }.files
                })
                val resolvedRuntimeFiles = project.layout.buildDirectory.file("parcelize-runtime-artifacts.txt")
                project.tasks.register("resolveParcelizeRuntime") { task ->
                    task.inputs.files(runtimeArtifacts)
                    task.outputs.file(resolvedRuntimeFiles)
                    task.doLast {
                        resolvedRuntimeFiles.get().asFile.writeText(runtimeArtifacts.files.map { it.name }.sorted().joinToString("\n"))
                    }
                }
            }

            build("resolveParcelizeRuntime")

            assertEquals(
                listOf("iosX64", "androidNativeArm32", "androidNativeArm64", "androidNativeX86", "androidNativeX64")
                    .map { target -> "parcelize-runtime-${target}Main-$KOTLIN_VERSION.klib" }
                    .sorted(),
                projectPath.resolve("build/parcelize-runtime-artifacts.txt").readLines(),
            )
        }
    }
}
