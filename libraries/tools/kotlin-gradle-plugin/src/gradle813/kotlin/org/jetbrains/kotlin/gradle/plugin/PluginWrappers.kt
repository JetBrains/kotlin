/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.diagnostics.CompilerDiagnosticsProblemsReporter
import org.jetbrains.kotlin.gradle.plugin.diagnostics.CompilerDiagnosticsProblemsReporterG813
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ProblemsReporter
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ProblemsReporterG813
import org.jetbrains.kotlin.gradle.plugin.internal.*

private const val PLUGIN_VARIANT_NAME = "gradle813"

open class KotlinPluginWrapper : AbstractKotlinPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinAndroidPluginWrapper : AbstractKotlinAndroidPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinMultiplatformPluginWrapper : AbstractKotlinMultiplatformPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinJsPluginWrapper : AbstractKotlinJsPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinApiPlugin : KotlinBaseApiPlugin() {
    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

private fun Project.registerVariantImplementations() {
    val factories = VariantImplementationFactoriesConfigurator.get(gradle)
    factories[BuildNeededDependentTasksWiringProvider.Factory::class] = BuildNeededDependentTaskWiringProviderG95.Factory()
    factories[CompilerDiagnosticsProblemsReporter.Factory::class] = CompilerDiagnosticsProblemsReporterG813.Factory()
    factories[ProblemsReporter.Factory::class] = ProblemsReporterG813.Factory()
}
