import gradle.GradlePluginVariant
import gradle.kotlin.dsl.accessors._5b414b80c33e971da78124b484e96576.dokka
import gradle.publishGradlePluginsJavadoc
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.dokka.gradle.DokkaExtension
import javax.inject.Inject

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

abstract class PluginApiReferenceExtension @Inject constructor(
    private val project: Project,
) {
    fun enableForAllGradlePluginVariants() {
        val variants = GradlePluginVariant.values()
        if (project.kotlinBuildProperties.publishGradlePluginsJavadoc) {
            variants.forEach { variant ->
                project.generateJavadocForPluginVariant(variant)
            }
        }
    }

    fun additionalDokkaConfiguration(configuration: DokkaExtension.() -> Unit) {
        if (!project.kotlinBuildProperties.publishGradlePluginsJavadoc) return
        project.dokka(configuration)
    }


    fun moduleName(name: String) {
        if (!project.kotlinBuildProperties.publishGradlePluginsJavadoc) return

        project.dokka {
            moduleName(name)
        }
    }

    fun embeddedProject(embedProject: ProjectDependency) {
        if (!project.kotlinBuildProperties.publishGradlePluginsJavadoc) return

        project.consumeEmbeddedSources(embedProject)
    }

    private var _failOnWarning: Boolean = false
    var failOnWarning: Boolean
        get() = _failOnWarning
        set(value) {
            if (project.kotlinBuildProperties.publishGradlePluginsJavadoc) {
                project.dokka {
                    this.dokkaPublications.configureEach {
                        this.failOnWarning.set(value)
                    }
                }
            }
        }
}
