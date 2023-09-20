/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.gradle.api.artifacts.ResolveException
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.mavenCentralCacheRedirector
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.ide.dependencyResolvers.resolveMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataDependencyResolution
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.util.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.fail

class StdlibJsExplicitDependencyResolutionTest {

    @Test
    fun `project with jvm and js targets - with stdlib-js dependency in commonMain - fails to resolve jvm compile classpath`() {
        checkKmpProjectResolvesAllMetadataConfigurationsAnd(
            configurationsThatMustFail = listOf(
                // jvm must not resolve, so that stdlib-js doesn't get onto jvm compile classpath
                "jvmCompileClasspath",
            ),
            configurationsThatMustResolve = listOf(
                "jsCompileClasspath"
            ) + resolvableMetadataConfigurations,
            sourceSetWithStdlibJs = "commonMain"
        ) {
            jvm()
            js()
        }
    }

    @Test
    fun `project with jvm and js targets - with stdlib-js dependency in jsMain - resolves js and jvm compile classpaths`() {
        checkKmpProjectResolvesAllMetadataConfigurationsAnd(
            configurationsThatMustResolve = listOf(
                "jsCompileClasspath",
                "jvmCompileClasspath",
            ) + resolvableMetadataConfigurations,
            sourceSetWithStdlibJs = "jsMain",
        ) {
            jvm()
            js()
        }
    }

    @Test
    fun `project js target - with stdlib-js dependency in jsMain - resolves js compile classpath`() {
        checkKmpProjectResolvesAllMetadataConfigurationsAnd(
            configurationsThatMustResolve = listOf("jsCompileClasspath") + resolvableMetadataConfigurations,
            sourceSetWithStdlibJs = "jsMain",
        ) {
            js()
        }
    }

    @Test
    fun `project with js target - with stdlib-js dependency in commonMain - resolves js compile classpath`() {
        checkKmpProjectResolvesAllMetadataConfigurationsAnd(
            configurationsThatMustResolve = listOf("jsCompileClasspath") + resolvableMetadataConfigurations,
            sourceSetWithStdlibJs = "commonMain",
        ) {
            js()
        }
    }

    private fun checkKmpProjectResolvesAllMetadataConfigurationsAnd(
        configurationsThatMustFail: List<String> = emptyList(),
        configurationsThatMustResolve: List<String>,
        sourceSetWithStdlibJs: String,
        configure: KotlinMultiplatformExtension.() -> Unit,
    ) {
        val project = buildProject {
            enableDependencyVerification(false)
            repositories.mavenLocal()
            repositories.mavenCentralCacheRedirector()
            applyMultiplatformPlugin()
            kotlin {
                configure()

                sourceSets.getByName(sourceSetWithStdlibJs) {
                    it.dependencies {
                        this.implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
                    }
                }
            }
        }
        project.evaluate()

        // Metadata resolution is always expected to resolve
        project.multiplatformExtension.sourceSets.forEach {
            assertDoesNotThrow {
                it.resolveMetadata<MetadataDependencyResolution>()
            }
        }

        configurationsThatMustFail.map {
            it to runCatching {
                project.configurations.getByName(it).resolve()
            }.exceptionOrNull()
        }
            .filterNot { (_, e) -> e is ResolveException }
            .takeIf { it.isNotEmpty() }
            ?.let { results ->
                fail(
                    "Expected configurations resolve to fail with ResolveException, but was\n" +
                            results.joinToString("\n") { (name, e) -> "  $name: ${e ?: "successful"}" }
                )
            }

        configurationsThatMustResolve.forEach {
            assertDoesNotThrow {
                project.configurations.getByName(it).resolve()
            }
        }
    }

    // These configurations were unresolvable due to KT-61126, but now they are resolvable
    private val resolvableMetadataConfigurations = listOf(
        "jsMainResolvableDependenciesMetadata",
        "jsTestResolvableDependenciesMetadata",
        "allSourceSetsCompileDependenciesMetadata",
    )

}
