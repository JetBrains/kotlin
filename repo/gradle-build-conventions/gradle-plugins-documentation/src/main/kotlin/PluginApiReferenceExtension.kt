import gradle.GradlePluginVariant
import gradle.publishGradlePluginsJavadoc
import org.gradle.api.Project
import org.jetbrains.dokka.gradle.GradleDokkaSourceSetBuilder
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import javax.inject.Inject

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class PluginApiReferenceExtension @Inject constructor(
    private val project: Project
) {
    fun enableForGradlePluginVariants(variants: Set<GradlePluginVariant>) {
        if (project.kotlinBuildProperties.publishGradlePluginsJavadoc) {
            variants.forEach { variant ->
                project.generateJavadocForPluginVariant(variant)
            }
        }
    }

    fun additionalDokkaConfiguration(configuration: GradleDokkaSourceSetBuilder.() -> Unit) {
        project.tasks.withType<AbstractDokkaLeafTask>().configureEach {
            dokkaSourceSets.configureEach(configuration)
        }
    }

    fun enableKotlinlangDocumentation() {
        project.configureTaskForKotlinlang()
    }

    private var _failOnWarning: Boolean = false
    var failOnWarning: Boolean
        get() = _failOnWarning
        set(value) {
            project.tasks.withType<AbstractDokkaLeafTask>().configureEach {
                failOnWarning.set(value)
            }
            _failOnWarning = value
        }
}