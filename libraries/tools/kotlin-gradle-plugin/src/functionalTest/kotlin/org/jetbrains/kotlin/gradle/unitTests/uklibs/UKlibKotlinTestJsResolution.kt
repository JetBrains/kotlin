@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.unitTests.uklibs

import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.KmpResolutionStrategy
import org.jetbrains.kotlin.gradle.util.*
import org.junit.Test
import kotlin.test.assertEquals

class UKlibKotlinTestJsResolution {

    @Test
    fun test() {
        val project = buildProjectWithMPP(
            preApplyCode = {
                setUklibPublicationStrategy()
                setUklibResolutionStrategy(KmpResolutionStrategy.InterlibraryUklibAndPSMResolution_PreferUklibs)
                enableDefaultStdlibDependency(false)
                enableDefaultJsDomApiDependency(false)
            }
        ) {
            configureRepositoriesForTests()
            kotlin {
                jvm()
                linuxArm64()
                js()
                wasmJs()
                wasmWasi()
                val stdlibVersion = project.getKotlinPluginVersion()
                val kotlinTestJs = dependencies.create("org.jetbrains.kotlin:kotlin-test-js:${stdlibVersion}") {
                    isTransitive = false
                }
                dependencies {
                    implementation.add(kotlinTestJs)
                }
            }
        }.evaluate()

        fun resolvedVariantNames(configurationName: String): List<String> = project.configurations.getByName(configurationName)
            .incoming.resolutionResult.allComponents.map { (it as ResolvedComponentResult).variants.single().displayName }

        fun KotlinTarget.resolvedMainCompilation(): List<String> =
            resolvedVariantNames(compilations.getByName("main").compileDependencyConfigurationName)

        fun KotlinTarget.resolvedMainRuntime(): List<String> =
            resolvedVariantNames(compilations.getByName("main").runtimeDependencyConfigurationName!!)

        assertEquals(
            listOf("jsCompileClasspath", "jsApiElements"),
            project.multiplatformExtension.js().resolvedMainCompilation(),
        )
        assertEquals(
            listOf("jsRuntimeClasspath", "jsRuntimeElements"),
            project.multiplatformExtension.js().resolvedMainRuntime(),
        )

        assertEquals(
            listOf("jvmCompileClasspath", "stubKotlinTestJsFallback"),
            project.multiplatformExtension.jvm().resolvedMainCompilation(),
        )
        assertEquals(
            listOf("jvmRuntimeClasspath", "stubKotlinTestJsFallback"),
            project.multiplatformExtension.jvm().resolvedMainRuntime(),
        )
        assertEquals(
            listOf("wasmJsCompileClasspath", "stubKotlinTestJsFallback"),
            project.multiplatformExtension.wasmJs().resolvedMainCompilation(),
        )
        assertEquals(
            listOf("wasmJsRuntimeClasspath", "stubKotlinTestJsFallback"),
            project.multiplatformExtension.wasmJs().resolvedMainRuntime(),
        )
        assertEquals(
            listOf("wasmWasiCompileClasspath", "stubKotlinTestJsFallback"),
            project.multiplatformExtension.wasmWasi().resolvedMainCompilation(),
        )
        assertEquals(
            listOf("wasmWasiRuntimeClasspath", "stubKotlinTestJsFallback"),
            project.multiplatformExtension.wasmWasi().resolvedMainRuntime(),
        )
        assertEquals(
            listOf("linuxArm64CompileKlibraries", "stubKotlinTestJsFallback"),
            project.multiplatformExtension.linuxArm64().resolvedMainCompilation(),
        )
    }

}