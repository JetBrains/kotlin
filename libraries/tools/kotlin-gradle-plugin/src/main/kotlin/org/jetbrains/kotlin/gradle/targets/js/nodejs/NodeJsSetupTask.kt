package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import java.io.File
import java.net.URI

@CacheableTask
open class NodeJsSetupTask : DefaultTask() {
    private val settings = NodeJsRootPlugin.apply(project.rootProject)
    private val env by lazy { settings.requireConfigured() }

    val ivyDependency: String
        @Input get() = env.ivyDependency

    val destination: File
        @OutputDirectory get() = env.nodeDir

    init {
        @Suppress("LeakingThis")
        onlyIf {
            settings.download && !File(env.nodeExecutable).isFile
        }
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        @Suppress("UnstableApiUsage", "DEPRECATION")
        val repo = project.repositories.ivy { repo ->
            repo.name = "Node Distributions at ${settings.nodeDownloadBaseUrl}"
            repo.url = URI(settings.nodeDownloadBaseUrl)

            repo.patternLayout {
                it.artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
                it.ivy("v[revision]/ivy.xml")
            }
            repo.metadataSources { it.artifact() }
            repo.content { it.includeModule("org.nodejs", "node") }
        }

        val dep = this.project.dependencies.create(ivyDependency)
        val conf = this.project.configurations.detachedConfiguration(dep)
        conf.isTransitive = false

        val startDownloadTime = System.currentTimeMillis()
        val result = conf.resolve().single()

        val downloadDuration = System.currentTimeMillis() - startDownloadTime
        if (downloadDuration > 0) {
            KotlinBuildStatsService.getInstance()
                ?.report(NumericalMetrics.ARTIFACTS_DOWNLOAD_SPEED, result.length() * 1000 / downloadDuration)
        }

        project.repositories.remove(repo)

        project.logger.kotlinInfo("Using node distribution from '$result'")

        unpackNodeArchive(result, destination.parentFile) // parent because archive contains name already

        if (!env.isWindows) {
            File(env.nodeExecutable).setExecutable(true)
        }
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
        const val NAME: String = "kotlinNodeJsSetup"
    }
}
