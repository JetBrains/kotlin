package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.repositories.IvyPatternRepositoryLayout
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import java.io.File
import java.net.URI

open class NodeJsSetupTask : DefaultTask() {
    private val settings = NodeJsExtension[project]
    private val env by lazy { settings.buildEnv() }

    init {
        group = NodeJsExtension.NODE_JS
        description = "Download and install a local node/npm version."
    }

    val input: Set<String>
        @Input get() = setOf(settings.download.toString(), env.ivyDependency)

    val destination: File
        @OutputDirectory get() = env.nodeDir

    @Suppress("unused")
    @TaskAction
    fun exec() {
        if (!settings.download)
            return

        @Suppress("UnstableApiUsage", "DEPRECATION")
        val repo = project.repositories.ivy { repo ->
            repo.name = "Node Distributions at ${settings.distBaseUrl}"
            repo.url = URI(settings.distBaseUrl)

            if (isGradleVersionAtLeast(5, 0)) {
                repo.patternLayout { layout ->
                    configureNodeJsIvyPatternLayout(layout)
                }
            } else {
                repo.layout("pattern") { layout ->
                    configureNodeJsIvyPatternLayout(layout as IvyPatternRepositoryLayout)
                }
            }
            repo.metadataSources { it.artifact() }

            if (isGradleVersionAtLeast(5, 1)) {
                repo.content { it.includeModule("org.nodejs", "node") }
            }
        }

        val dep = this.project.dependencies.create(env.ivyDependency)
        val conf = this.project.configurations.detachedConfiguration(dep)
        conf.isTransitive = false
        val result = conf.resolve().single()
        project.repositories.remove(repo)

        project.logger.kotlinInfo("Using node distribution from '$result'")

        unpackNodeArchive(result, destination.parentFile) // parent because archive contains name already

        if (!env.isWindows) {
            File(env.nodeExec).setExecutable(true)
        }
    }

    private fun configureNodeJsIvyPatternLayout(layout: IvyPatternRepositoryLayout) {
        layout.artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
        layout.ivy("v[revision]/ivy.xml")
    }

    private fun unpackNodeArchive(archive: File, destination: File) {
        project.logger.kotlinInfo("Unpacking $archive to $destination")

        when {
            archive.name.endsWith("zip") -> project.copy {
                it.from(project.zipTree(archive))
                it.into(destination)
            }
            else -> {
                project.copy {
                    it.from(project.tarTree(archive))
                    it.into(destination)
                }
            }
        }
    }

    companion object {
        const val NAME: String = "nodeJsSetup"
    }
}
