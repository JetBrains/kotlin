/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName")
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmCompilerPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinLinuxX64Variant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.jvm
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerPluginData
import org.jetbrains.kotlin.project.model.*
import org.junit.Test
import kotlin.test.assertEquals

class KpmCompilerPluginTest {
    @Test
    fun `basic plugin with kpm`() {
        class TestPlugin : FakeKpmPlugin(
            id = "test",
            options = mapOf("a" to "b"),
            metadataArtifact = "metadata",
            metadataNativeArtifact = "metadata-native",
            platformArtifact = "platform"
        )

        /**
         * Applies plugin only for Native Variant compilation
         * nothing for fragment metadata compilation
         */
        class NativeOnly : FakeKpmPlugin(
            id = "native-only",
            options = mapOf("a" to "c"),
            platformArtifact = "native-only"
        ) {
            override fun forPlatformCompilation(variant: KotlinModuleVariant): PluginData? =
                if (variant.platform == KotlinPlatformTypeAttribute.NATIVE) {
                    super.forPlatformCompilation(variant)
                } else {
                    null
                }
        }

        val project = buildProjectWithKPM {
            plugins.apply(TestPlugin::class.java)
            plugins.apply(NativeOnly::class.java)

            projectModel {
                main {
                    jvm
                    val linuxX64 = fragments.create("linuxX64", KotlinLinuxX64Variant::class.java)
                    val jvmAndLinux = fragments.create("jvmAndLinux")

                    jvm.refines(jvmAndLinux)
                    linuxX64.refines(jvmAndLinux)
                }
            }
        }
        project.evaluate()

        project.pluginDataOfTask("compileCommonMainKotlinMetadata").run {
            assertEquals(setOf("metadata"), artifacts())
            assertEquals(listOf("plugin:test:a=b"), options.arguments)
        }

        project.pluginDataOfTask("compileCommonMainKotlinNativeMetadata").run {
            assertEquals(setOf("metadata-native"), artifacts())
            assertEquals(listOf("plugin:test:a=b"), options.arguments)
        }

        project.pluginDataOfTask("compileKotlinJvm").run {
            assertEquals(setOf("platform"), artifacts())
            assertEquals(listOf("plugin:test:a=b"), options.arguments)
        }

        project.pluginDataOfTask("compileKotlinLinuxX64").run {
            assertEquals(setOf("platform", "native-only"), artifacts())
            assertEquals(listOf("plugin:test:a=b", "plugin:native-only:a=c"), options.arguments)
        }
    }

    private fun Project.pluginDataOfTask(taskName: String) = this
        .tasks
        .getByName(taskName)
        .let {
            when(it) {
                is AbstractKotlinCompile<*> -> it.kotlinPluginData
                is AbstractKotlinNativeCompile<*, *> -> it.kotlinPluginData
                else -> error("Unknown task type: $it")
            }
        }
        ?.get()
        ?: error("Plugin Data not found")

    private fun KotlinCompilerPluginData.artifacts() = classpath
        .let { it as Configuration }
        .allDependencies
        .map { it.name }
        .toSet()

    private open class FakeKpmPlugin(
        val id: String,
        val options: Map<String, String> = emptyMap(),
        val metadataArtifact: String? = null,
        val metadataNativeArtifact: String? = null,
        val platformArtifact: String? = null
    ) : KpmCompilerPlugin, GradleKpmCompilerPlugin {
        override fun apply(target: Project) = Unit
        override val kpmCompilerPlugin get() = this

        private fun pluginData(artifact: String) = PluginData(
            pluginId = id,
            artifact = PluginData.ArtifactCoordinates(
                group = "test",
                artifact = artifact
            ),
            options = options.map { StringOption(it.key, it.value) }
        )

        override fun forMetadataCompilation(fragment: KotlinModuleFragment) = metadataArtifact?.let(::pluginData)

        override fun forNativeMetadataCompilation(fragment: KotlinModuleFragment) = metadataNativeArtifact?.let(::pluginData)

        override fun forPlatformCompilation(variant: KotlinModuleVariant) = platformArtifact?.let(::pluginData)

    }
}