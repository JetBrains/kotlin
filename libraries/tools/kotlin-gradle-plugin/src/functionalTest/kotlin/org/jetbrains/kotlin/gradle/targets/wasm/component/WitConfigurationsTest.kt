/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.wasm.component

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.wasm.component.internal.witDirectories
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableDefaultJsDomApiDependency
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WitConfigurationsTest {

    /**
     * Mirrors `wasmWasiStdlibImplResolvable` of the Kotlin build:
     * a configuration that requests the klib attributes without the artifact type must not see the WIT declarations.
     */
    @Test
    fun `a resolvable configuration requesting the runtime usage resolves a WASI project dependency to its klib`() {
        val (lib, app) = buildLibAndApp()

        assertEquals(
            lib.configurations.getByName("wasmWasiRuntimeElements").artifacts.files.files,
            app.stdlibImplResolvableOnLib().incoming.artifacts.artifactFiles.files,
        )
    }

    /**
     * The WIT declarations must be excluded by their usage, not by a tie-breaker on attributes the consumer doesn't request.
     * With the klib packaging attribute, the WIT declarations variant can't be told from the klib by that tie-breaker.
     */
    @Test
    fun `WIT declarations are not selected for the runtime usage even with the klib packaging attribute`() {
        val (lib, app) = buildLibAndApp()
        lib.configurations.getByName("wasmWasiRuntimeElements").outgoing.variants.getByName("wit").attributes.attribute(
            KlibPackaging.ATTRIBUTE,
            lib.objects.named(KlibPackaging::class.java, KlibPackaging.PACKED),
        )

        assertEquals(
            lib.configurations.getByName("wasmWasiRuntimeElements").artifacts.files.files,
            app.stdlibImplResolvableOnLib().incoming.artifacts.artifactFiles.files,
        )
    }

    @Test
    fun `runtime classpath resolves a WASI project dependency to its klib`() {
        val (lib, app) = buildLibAndApp()

        assertEquals(
            lib.wasmWasiMainCompilation().output.classesDirs.files,
            app.configurations.getByName("wasmWasiRuntimeClasspath").incoming.artifacts.artifactFiles.files,
        )
    }

    @Test
    fun `WIT directories of a WASI project dependency are its wit directory`() {
        val (lib, app) = buildLibAndApp()

        assertEquals(
            setOf(lib.layout.projectDirectory.dir(WIT_DIRECTORY_NAME).asFile),
            app.wasmWasiMainCompilation().witDirectories(app).files,
        )
    }

    @Test
    fun `a Java project declared in commonMain has no WIT directories`() {
        val root = buildProject(projectBuilder = { withName("root") })
        val javaLib = buildProject(projectBuilder = { withParent(root).withName("java-lib") }) {
            plugins.apply("java-library")
        }
        val app = buildProjectWithMPP(
            projectBuilder = { withParent(root).withName("app") },
            preApplyCode = { disableDefaultDependencies() },
        ) {
            kotlin {
                jvm()
                wasmWasi { wasmtime() }
                sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":java-lib"))
                }
            }
        }
        javaLib.evaluate()
        app.evaluate()

        assertEquals(
            emptySet(),
            app.wasmWasiMainCompilation().witDirectories(app).files,
        )
    }

    private fun buildLibAndApp(): Pair<Project, Project> {
        val root = buildProject(projectBuilder = { withName("root") })
        val lib = buildProjectWithMPP(
            projectBuilder = { withParent(root).withName("lib") },
            preApplyCode = { disableDefaultDependencies() },
        ) {
            kotlin { wasmWasi { wasmtime() } }
        }
        val app = buildProjectWithMPP(
            projectBuilder = { withParent(root).withName("app") },
            preApplyCode = { disableDefaultDependencies() },
        ) {
            kotlin {
                wasmWasi { wasmtime() }
                sourceSets.getByName("wasmWasiMain").dependencies {
                    implementation(project(":lib"))
                }
            }
        }
        lib.evaluate()
        app.evaluate()
        return lib to app
    }

    /** Mirrors `wasmWasiStdlibImplResolvable` of the Kotlin build, with a dependency on the `lib` project. */
    private fun Project.stdlibImplResolvableOnLib(): Configuration {
        val stdlibImpl = configurations.dependencyScope("wasmWasiStdlibImpl")
        val stdlibImplResolvable = configurations.resolvable("wasmWasiStdlibImplResolvable") { configuration ->
            configuration.extendsFrom(stdlibImpl.get())
            configuration.attributes { attributes ->
                attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_RUNTIME))
                attributes.attribute(Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java), "wasm")
                attributes.attribute(Attribute.of("org.jetbrains.kotlin.wasm.target", String::class.java), "wasi")
            }
        }
        dependencies.add(stdlibImpl.name, dependencies.project(mapOf("path" to ":lib")))
        return stdlibImplResolvable.get()
    }

    private fun Project.wasmWasiMainCompilation() =
        multiplatformExtension.wasmWasi().compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    /** No repositories are declared, so the default dependencies can't be resolved. */
    private fun Project.disableDefaultDependencies() {
        enableDefaultStdlibDependency(false)
        enableDefaultJsDomApiDependency(false)
    }
}
