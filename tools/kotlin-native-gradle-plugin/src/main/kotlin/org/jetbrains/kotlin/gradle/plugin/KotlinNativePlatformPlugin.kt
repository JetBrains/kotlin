package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask

open class KotlinNativePlatformPlugin: KotlinPlatformImplementationPluginBase("native") {

    protected val commonProjects = arrayListOf<Project>()

    override fun apply(project: Project) {
        val expectedByConfig = project.configurations.create(EXPECTED_BY_CONFIG_NAME)
        expectedByConfig.isTransitive = false
        expectedByConfig.dependencies.whenObjectAdded { dep ->
            if (dep is ProjectDependency) {
                addCommonProject(dep.dependencyProject, project)
            } else {
                throw GradleException("$project '${expectedByConfig.name}' dependency is not a project: $dep")
            }
        }
    }

    protected fun addCommonProject(commonProject: Project, platformProject: Project) {
        commonProjects.add(commonProject)
        commonProject.whenEvaluated {
            if (!commonProject.pluginManager.hasPlugin("kotlin-platform-common")) {
                throw GradleException("Platform project $platformProject has an " +
                        "'$EXPECTED_BY_CONFIG_NAME' dependency to non-common project $commonProject")
            }

            platformProject.tasks
                    .withType(KonanCompileTask::class.java)
                    .filter { it.enableMultiplatform }
                    .forEach { task: KonanCompileTask ->
                        task.commonSourceSets.forEach { commonSourceSetName ->
                            val commonSourceSet = commonProject.sourceSets.findByName(commonSourceSetName) ?:
                            throw GradleException("Cannot find a source set with name '$commonSourceSetName' " +
                                    "in a common project '${commonProject.path}' " +
                                    "for an artifact '${task.artifactName}' " +
                                    "in a platform project '${platformProject.path}'")

                            commonSourceSet.kotlin!!.srcDirs.forEach {
                                task.commonSrcDir(it)
                            }
                        }
            }
        }
    }

    protected val Project.sourceSets: SourceSetContainer
        get() = convention.getPlugin(JavaPluginConvention::class.java).sourceSets
}
