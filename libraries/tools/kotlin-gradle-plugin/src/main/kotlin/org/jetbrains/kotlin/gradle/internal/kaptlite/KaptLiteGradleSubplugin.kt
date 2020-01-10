/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kaptlite

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

class KaptLiteGradleSubplugin : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(KaptLiteGradleSubplugin::class.java) != null
    }

    override fun apply(project: Project) {
        KaptLiteKotlinGradleSubplugin().run {
            project.configurations.create(KaptLiteKotlinGradleSubplugin.JAVAC_PLUGIN_CONFIGURATION_NAME).apply {
                val kotlinPluginVersion = project.getKotlinPluginVersion()
                if (kotlinPluginVersion != null) {
                    dependencies.add(project.dependencies.create("org.jetbrains.kotlin:kapt-lite-javac-plugin:$kotlinPluginVersion"))
                } else {
                    project.logger.error("Kotlin plugin should be enabled before 'kotlin-kapt-lite'")
                }
            }
        }
    }
}

class KaptLiteKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val JAVAC_PLUGIN_CONFIGURATION_NAME = "kotlinKaptLiteJavacPluginClasspath"
    }

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean {
        return task is KotlinCompile && KaptLiteGradleSubplugin.isEnabled(project)
    }

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {
        fun reportError(message: String) = project.logger.error(project.path + ": " + message)

        val baseName = getBaseName(kotlinCompile)

        if (Kapt3GradleSubplugin.isEnabled(project)) {
            reportError("'kotlin-kapt' and 'kotlin-kapt-lite' plugins can not work together. Remove one.")
            return emptyList()
        }

        if (javaCompile !is JavaCompile) {
            reportError("Java compile task is absent or incompatible. Kapt-lite requires 'JavaCompile' in order to work properly.")
            return emptyList()
        }

        val kotlinPluginVersion = project.getKotlinPluginVersion()
        if (kotlinPluginVersion == null) {
            reportError("Kotlin plugin is not enabled.")
            return emptyList()
        }

        val javacProcessorDependencies = javaCompile.options.annotationProcessorPath
        if (javacProcessorDependencies == null) {
            reportError("Annotation processor dependencies are not set.")
            return emptyList()
        }

        run /* Register Javac plugin */ {
            val javacPluginConfiguration = project.configurations.getByName(JAVAC_PLUGIN_CONFIGURATION_NAME)
            javaCompile.options.annotationProcessorPath = javacProcessorDependencies + javacPluginConfiguration
            javaCompile.options.compilerArgs.add("-Xplugin:KaptJavacPlugin")
        }

        val stubsOutputDir = getKaptStubsOutputPath(project, baseName)
        javaCompile.source(stubsOutputDir)

        return listOf(
            FilesSubpluginOption("stubs", listOf(stubsOutputDir))
        )
    }

    private fun getKaptStubsOutputPath(project: Project, baseName: String): File {
        val dirNameForSourceSet = if (baseName.isEmpty()) "main" else baseName
        return File(project.buildDir, "tmp/kaptLite/stubs/$dirNameForSourceSet")
    }

    private fun getBaseName(kotlinCompile: AbstractCompile): String {
        val name = kotlinCompile.name
        assert(name.startsWith("compile") && name.endsWith("Kotlin"))
        return name.drop("compile".length).dropLast("Kotlin".length).decapitalize()
    }

    override fun getCompilerPluginId(): String = "org.jetbrains.kotlin.kaptlite"

    override fun getPluginArtifact(): SubpluginArtifact {
        return SubpluginArtifact("org.jetbrains.kotlin", "kapt-lite-compiler-plugin-embeddable")
    }
}