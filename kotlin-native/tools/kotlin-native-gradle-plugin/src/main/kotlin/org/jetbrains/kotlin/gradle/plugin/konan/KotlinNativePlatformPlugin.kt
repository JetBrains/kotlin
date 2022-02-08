package org.jetbrains.kotlin.gradle.plugin.konan

import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformImplementationPluginBase
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask
import javax.inject.Inject

open class KotlinNativePlatformPlugin: KotlinPlatformImplementationPluginBase("native") {

    private val Project.konanMultiplatformTasks: Collection<KonanCompileTask>
        get() = tasks.withType(KonanCompileTask::class.java).filter { it.enableMultiplatform }

    open class RequestedCommonSourceSet @Inject constructor(private val name: String): Named {
        override fun getName() = name
    }

    override fun addCommonSourceSetToPlatformSourceSet(commonSourceSet: Named, platformProject: Project) {
        val commonSourceSetName = commonSourceSet.name

        platformProject.konanMultiplatformTasks
            .filter { it.commonSourceSets.contains(commonSourceSetName) }
            .forEach { task: KonanCompileTask ->
                getKotlinSourceDirectorySetSafe(commonSourceSet)!!.srcDirs.forEach {
                    task.commonSrcDir(it)
                }
            }
    }

    override fun namedSourceSetsContainer(project: Project): NamedDomainObjectContainer<*> =
        project.container(RequestedCommonSourceSet::class.java).apply {
            project.konanMultiplatformTasks.forEach { task ->
                task.commonSourceSets.forEach { maybeCreate(it) }
            }
        }
}
