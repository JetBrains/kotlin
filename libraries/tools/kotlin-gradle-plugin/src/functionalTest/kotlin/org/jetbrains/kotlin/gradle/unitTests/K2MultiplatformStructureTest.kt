/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DEPRECATION")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentAware
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerArgumentsProducer.CreateCompilerArgumentsContext.Companion.lenient
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure.Fragment
import org.jetbrains.kotlin.gradle.tasks.K2MultiplatformStructure.RefinesEdge
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.main
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class K2MultiplatformStructureTest {
    private val project = buildProject {
        /* Resolving dependencies is necessary for creating compiler arguments */
        enableDefaultStdlibDependency(false)
        applyMultiplatformPlugin()
    }
    private val kotlin = project.multiplatformExtension

    @Test
    fun `test - configureK2Multiplatform - then parse arguments`() {
        val structure = project.objects.newInstance<K2MultiplatformStructure>()
        structure.refinesEdges.set(listOf(RefinesEdge("a", "b"), RefinesEdge("b", "c")))
        structure.fragments.set(
            listOf(
                Fragment("a", project.files("a.kt")),
                Fragment("b", project.files("b.kt")),
                Fragment("c", project.files())
            )
        )

        val sourceArguments = K2JVMCompilerArguments()
        sourceArguments.configureK2Multiplatform(structure)

        val parsedArguments = K2JVMCompilerArguments().apply {
            parseCommandLineArguments(ArgumentUtils.convertArgumentsToStringList(sourceArguments), this)
        }

        val fragments = parsedArguments.fragments ?: fail("Missing ${CommonCompilerArguments::fragments.name}")
        assertEquals(listOf("a", "b", "c"), fragments.toList())

        val fragmentSources = parsedArguments.fragmentSources ?: fail("Missing ${CommonCompilerArguments::fragmentSources.name}")
        assertEquals(
            listOf(
                "a:${project.file("a.kt").absolutePath}",
                "b:${project.file("b.kt").absolutePath}"
            ),
            fragmentSources.toList()
        )

        val dependsOn = parsedArguments.fragmentRefines ?: fail("Missing ${CommonCompilerArguments::fragmentRefines.name}")
        assertEquals(listOf("a:b", "b:c"), dependsOn.toList())
    }

    @Test
    fun `test - configure jvm compilation`() {
        `test compilations multiplatformStructure configuration`(kotlin.jvm().compilations.main)
    }

    @Test
    fun `test - configure native compilation`() {
        `test compilations multiplatformStructure configuration`(kotlin.linuxX64().compilations.main)
    }

    @Test
    fun `test - configure js compilation`() {
        `test compilations multiplatformStructure configuration`(kotlin.js(IR).compilations.main)
    }

    private fun `test compilations multiplatformStructure configuration`(compilation: KotlinCompilation<*>) {
        val defaultSourceSet = compilation.defaultSourceSet
        /* Create an additional intermediate source set for testing */
        kotlin.sourceSets.create("intermediateMain") { intermediateMain ->
            intermediateMain.dependsOn(kotlin.sourceSets.getByName("commonMain"))
            compilation.defaultSourceSet.dependsOn(intermediateMain)
        }

        /* Create dummy source files */
        kotlin.sourceSets.all { sourceSet ->
            sourceSet.kotlin.srcDirs.first().resolve("${sourceSet.name}.kt").apply {
                parentFile.mkdirs()
                writeText("object ${sourceSet.name}")
            }
        }

        /* Enable K2 if necessary */
        if (KotlinVersion.DEFAULT < KotlinVersion.KOTLIN_2_0) {
            compilation.compilerOptions.options.languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }

        val compileTask = compilation.compileTaskProvider.get() as K2MultiplatformCompilationTask

        /* check dependsOnEdges */
        assertEquals(
            setOf(
                RefinesEdge(defaultSourceSet.name, "intermediateMain"),
                RefinesEdge("intermediateMain", "commonMain")
            ),
            compileTask.multiplatformStructure.refinesEdges.get().toSet()
        )

        /* check source files */
        assertEquals(
            mapOf(
                "commonMain" to project.files("src/commonMain/kotlin/commonMain.kt").toSet(),
                "intermediateMain" to project.files("src/intermediateMain/kotlin/intermediateMain.kt").toSet(),
                defaultSourceSet.name to project.files("src/${defaultSourceSet.name}/kotlin/${defaultSourceSet.name}.kt").toSet()
            ),
            compileTask.multiplatformStructure.fragments.get().associate { fragment ->
                fragment.fragmentName to fragment.sources.files
            }
        )

        val args = compileTask.buildCompilerArguments()

        if (args.commonSources != null) {
            fail("Unexpected ${CommonCompilerArguments::commonSources.name} in K2 compilation: ${args.commonSources}")
        }

        if (args.fragments == null) {
            fail("Missing ${CommonCompilerArguments::fragments.name} in K2 compilation")
        }

        if (args.fragmentSources == null) {
            fail("Missing ${CommonCompilerArguments::fragmentSources.name} in K2 compilation")
        }

        if (args.fragmentRefines == null) {
            fail("Missing ${CommonCompilerArguments::fragmentRefines.name} in K2 compilation")
        }
    }
}

private fun K2MultiplatformCompilationTask.buildCompilerArguments(): CommonCompilerArguments {
    /* KotlinNative implements CompilerArgumentAware, but does not adhere to its contract */
    @Suppress("UNCHECKED_CAST")
    this as CompilerArgumentAware<CommonCompilerArguments>
    return this.createCompilerArguments(lenient)
}

internal fun CommonCompilerArguments.configureK2Multiplatform(multiplatformStructure: K2MultiplatformStructure) {
    fragments = multiplatformStructure.fragmentsCompilerArgs
    fragmentSources = multiplatformStructure.fragmentSourcesCompilerArgs()
    fragmentRefines = multiplatformStructure.fragmentRefinesCompilerArgs
}
