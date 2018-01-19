/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.BaseExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

abstract class KotlinPlatformPluginBase(protected val platformName: String) : Plugin<Project> {
    companion object {
        @JvmStatic
        protected inline fun <reified T : Plugin<*>> Project.applyPlugin() {
            pluginManager.apply(T::class.java)
        }
    }
}

open class KotlinPlatformCommonPlugin : KotlinPlatformPluginBase("common") {
    override fun apply(project: Project) {
        project.applyPlugin<KotlinCommonPluginWrapper>()
    }
}

const val EXPECTED_BY_CONFIG_NAME = "expectedBy"

const val IMPLEMENT_CONFIG_NAME = "implement"
const val IMPLEMENT_DEPRECATION_WARNING = "The '$IMPLEMENT_CONFIG_NAME' configuration is deprecated and will be removed. " +
                                          "Use '$EXPECTED_BY_CONFIG_NAME' instead."

open class KotlinPlatformImplementationPluginBase(platformName: String) : KotlinPlatformPluginBase(platformName) {
    private val commonProjects = arrayListOf<Project>()
    private val platformKotlinTasksBySourceSetName = hashMapOf<String, AbstractKotlinCompile<*>>()

    override fun apply(project: Project) {
        project.tasks.withType(AbstractKotlinCompile::class.java).all {
            (it as KotlinCompile<*>).kotlinOptions.freeCompilerArgs += listOf("-Xmulti-platform")
        }

        project.tasks.filterIsInstance<AbstractKotlinCompile<*>>().associateByTo(platformKotlinTasksBySourceSetName) { it.sourceSetName }

        val implementConfig = project.configurations.create(IMPLEMENT_CONFIG_NAME)
        val expectedByConfig = project.configurations.create(EXPECTED_BY_CONFIG_NAME)

        implementConfig.dependencies.whenObjectAdded {
            if (!implementConfigurationIsUsed) {
                implementConfigurationIsUsed = true
                project.logger.kotlinWarn(IMPLEMENT_DEPRECATION_WARNING)
            }
        }

        listOf(implementConfig, expectedByConfig).forEach { config ->
            config.isTransitive = false

            config.dependencies.whenObjectAdded { dep ->
                if (dep is ProjectDependency) {
                    addCommonProject(dep.dependencyProject, project)
                }
                else {
                    throw GradleException("$project '${config.name}' dependency is not a project: $dep")
                }
            }
        }

        val incrementalMultiplatform = PropertiesProvider(project).incrementalMultiplatform ?: true
        project.afterEvaluate {
            project.tasks.withType(AbstractKotlinCompile::class.java).all {
                if (it.incremental && !incrementalMultiplatform) {
                    project.logger.debug("IC is turned off for task '${it.path}' because multiplatform IC is not enabled")
                }
                it.incremental = it.incremental && incrementalMultiplatform
            }
        }
    }

    private var implementConfigurationIsUsed = false

    private fun addCommonProject(commonProject: Project, platformProject: Project) {
        commonProjects.add(commonProject)
        if (commonProjects.size > 1) {
            throw GradleException(
                    "Platform project $platformProject has more than one " +
                    "'$EXPECTED_BY_CONFIG_NAME'${if (implementConfigurationIsUsed) "/'$IMPLEMENT_CONFIG_NAME'" else ""} " +
                    "dependency: ${commonProjects.joinToString()}")
        }

        commonProject.whenEvaluated {
            if (!commonProject.pluginManager.hasPlugin("kotlin-platform-common")) {
                throw GradleException(
                        "Platform project $platformProject has an " +
                        "'$EXPECTED_BY_CONFIG_NAME'${if (implementConfigurationIsUsed) "/'$IMPLEMENT_CONFIG_NAME'" else ""} " +
                        "dependency to non-common project $commonProject")
            }

            commonProject.sourceSets.all { commonSourceSet ->
                // todo: warn if not found
                addCommonSourceSetToPlatformSourceSet(commonSourceSet, platformProject)
            }
        }
    }

    protected open fun addCommonSourceSetToPlatformSourceSet(commonSourceSet: SourceSet, platformProject: Project) {
        val platformTask = platformKotlinTasksBySourceSetName[commonSourceSet.name]
        commonSourceSet.kotlin!!.srcDirs.forEach { platformTask?.source(it) }
    }

    protected val SourceSet.kotlin: SourceDirectorySet?
        get() {
            // Access through reflection, because another project's KotlinSourceSet might be loaded
            // by a different class loader:
            val convention = (getConvention("kotlin") ?: getConvention("kotlin2js")) ?: return null
            val kotlinSourceSetIface = convention.javaClass.interfaces.find { it.name == KotlinSourceSet::class.qualifiedName }
            val getKotlin = kotlinSourceSetIface?.methods?.find { it.name == "getKotlin" } ?: return null
            return getKotlin(convention) as? SourceDirectorySet
        }

    companion object {
        @JvmStatic
        protected fun <T> Project.whenEvaluated(fn: Project.() -> T) {
            if (state.executed) {
                fn()
            }
            else {
                afterEvaluate { it.fn() }
            }
        }
    }
}

open class KotlinPlatformAndroidPlugin : KotlinPlatformImplementationPluginBase("android") {
    override fun apply(project: Project) {
        project.applyPlugin<KotlinAndroidPluginWrapper>()
        super.apply(project)
    }

    override fun addCommonSourceSetToPlatformSourceSet(commonSourceSet: SourceSet, platformProject: Project) {
        val androidExtension = platformProject.extensions.getByName("android") as BaseExtension
        val androidSourceSet = androidExtension.sourceSets.findByName(commonSourceSet.name) ?: return
        val kotlinSourceSet = androidSourceSet.getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet ?: return
        kotlinSourceSet.kotlin.source(commonSourceSet.kotlin!!)
    }
}

open class KotlinPlatformJvmPlugin : KotlinPlatformImplementationPluginBase("jvm") {
    override fun apply(project: Project) {
        project.applyPlugin<KotlinPluginWrapper>()
        super.apply(project)
    }
}

open class KotlinPlatformJsPlugin : KotlinPlatformImplementationPluginBase("js") {
    override fun apply(project: Project) {
        project.applyPlugin<Kotlin2JsPluginWrapper>()
        super.apply(project)
    }
}

private val Project.sourceSets: SourceSetContainer
    get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets
