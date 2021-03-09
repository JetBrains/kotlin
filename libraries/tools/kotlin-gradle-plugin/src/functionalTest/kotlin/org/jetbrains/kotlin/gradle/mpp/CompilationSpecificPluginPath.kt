/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName")

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class CompilationSpecificPluginPath {
    @Test
    fun `each compilation should have its own plugin classpath`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm("jvm1")
                jvm("jvm2")
                js("js") { browser() }
            }
        }

        // Expect to see plugin classpath for each target compilation
        listOf("jvm1", "jvm2", "js")
            .map(String::capitalize)
            .flatMap { listOf(pluginClassPathConfiguration(it, "main"), pluginClassPathConfiguration(it, "test")) }
            .plus(pluginClassPathConfiguration("metadata", "main")) // and also one for metadata
            .forEach { assertNotNull(project.configurations.findByName(it), "Configuration $it should exist") }
    }

    @Test
    fun `kotlin plugin author can control plugin artifacts per compilation`() {
        // Given platform-specific Kotlin plugins
        class JvmOnly : FakeSubPlugin("jvmOnly", null, { target.platformType == KotlinPlatformType.jvm })
        class JsOnly : FakeSubPlugin("jsOnly", null, { target.platformType == KotlinPlatformType.js })

        // When apply them to a Kotlin MPP Project
        val project = buildProjectWithMPP {
            plugins.apply(JvmOnly::class.java)
            plugins.apply(JsOnly::class.java)

            kotlin {
                jvm("desktop")
                js("web", IR) {
                    browser()
                }
            }
        }
        project.evaluate()

        // Then each plugin classpath should have its own dependency
        assertEquals(listOf("kotlin-scripting-compiler-embeddable", "jvmOnly"), project.subplugins("desktop"))
        assertEquals(listOf("kotlin-scripting-compiler-embeddable", "jsOnly"), project.subplugins("web"))

        // And each compilation task should have its own plugin classpath
        val compileDesktop = project.tasks.getByName("compileKotlinDesktop") as KotlinCompile
        val expectedConfig = project.configurations.getByName(pluginClassPathConfiguration("desktop", "main"))
        assertEquals(expectedConfig, compileDesktop.pluginClasspath)
    }

    @Test
    fun `native artifact should take precedence over regular artifact for native platform`() {
        // Given common plugin but with native-specific artifact
        class NativeSpecificPlugin : FakeSubPlugin("common", "native", { true })

        val project = buildProjectWithMPP {
            plugins.apply(NativeSpecificPlugin::class.java)

            kotlin {
                jvm()
                linuxX64()
            }
        }
        project.evaluate()

        assertTrue("common" in project.subplugins("jvm"))
        assertTrue("native" in project.subplugins("linuxX64"))
        assertTrue("common" !in project.subplugins("linuxX64"))
    }

    @Test
    fun `it should be possible to add common plugin classpath to all compilations except native`() {
        // Given MPP project
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                js { browser() }
                linuxX64("native")
            }
        }

        // When adding a shared plugin to common Plugin Classpath
        project.dependencies.add(PLUGIN_CLASSPATH_CONFIGURATION_NAME, "test:shared")
        project.evaluate()

        // Then ALL compilations should have shared plugin EXCEPT native
        assertTrue("shared" in project.subplugins("jvm"))
        assertTrue("shared" in project.subplugins("js"))
        assertTrue("shared" in project.subplugins("js", "test"))
        assertTrue("shared" in project.subplugins("metadata"))
        assertTrue("shared" !in project.subplugins("native"))
    }

    @Test
    fun `native compilations should have its own shared plugin classpath`() {
        // Given MPP project
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                linuxX64("linux")
                linuxX64("mac")
            }
        }

        // When adding a shared plugin to native Plugin Classpath
        project.dependencies.add(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME, "test:shared")
        project.evaluate()

        // Then ALL native compilations should have shared plugin
        assertTrue("shared" in project.subplugins("linux"))
        assertTrue("shared" in project.subplugins("mac"))
        assertTrue("shared" in project.subplugins("mac", "test"))

        // And all non-native should not have it
        assertTrue("shared" !in project.subplugins("metadata"))
        assertTrue("shared" !in project.subplugins("jvm"))
    }

    private fun pluginClassPathConfiguration(target: String, compilation: String) =
        "kotlinCompilerPluginClasspath${target.capitalize()}${compilation.capitalize()}"

    private fun Project.subplugins(target: String, compilation: String = "main") = this
        .configurations
        .getByName(pluginClassPathConfiguration(target, compilation))
        .allDependencies
        .map { it.name }


    private fun buildProjectWithMPP(code: Project.() -> Unit): ProjectInternal {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("kotlin-multiplatform")
        project.code()
        return project as ProjectInternal
    }

    private fun Project.kotlin(code: KotlinMultiplatformExtension.() -> Unit) {
        val kotlin = project.kotlinExtension as KotlinMultiplatformExtension
        kotlin.code()
    }

    private abstract class FakeSubPlugin(
        val id: String,
        val idNative: String? = null,
        val isApplicablePredicate: KotlinCompilation<*>.() -> Boolean
    ) : KotlinCompilerPluginSupportPlugin {
        override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = kotlinCompilation.isApplicablePredicate()

        override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
            kotlinCompilation.target.project.provider { emptyList<SubpluginOption>() }

        override fun getCompilerPluginId(): String = id

        override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
            "test",
            id
        )

        override fun getPluginArtifactForNative(): SubpluginArtifact? = idNative?.let {
            SubpluginArtifact(
                "test",
                it
            )
        }
    }
}