package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSetProviderImpl
import org.jetbrains.kotlin.gradle.tasks.AndroidTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import java.io.FileNotFoundException
import java.util.*
import javax.inject.Inject

abstract class KotlinBasePluginWrapper(protected val fileResolver: FileResolver): Plugin<Project> {
    private val log = Logging.getLogger(this.javaClass)
    protected val kotlinPluginVersion = loadKotlinVersionFromResource(log)

    override fun apply(project: Project) {
        // TODO: consider only set if if daemon or parallel compilation are enabled, though this way it should be safe too
        System.setProperty(org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")

        val plugin = getPlugin()
        plugin.apply(project)

        KotlinGradleBuildServices.init(project.gradle)
    }

    protected abstract fun getPlugin(): Plugin<Project>
}

open class KotlinPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin() =
            KotlinPlugin(KotlinTasksProvider(), KotlinSourceSetProviderImpl(fileResolver), kotlinPluginVersion)
}

open class KotlinAndroidPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin() =
            KotlinAndroidPlugin(AndroidTasksProvider(), KotlinSourceSetProviderImpl(fileResolver), kotlinPluginVersion)
}

open class Kotlin2JsPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin() =
            Kotlin2JsPlugin(KotlinTasksProvider(), KotlinSourceSetProviderImpl(fileResolver), kotlinPluginVersion)
}

private fun Any.loadKotlinVersionFromResource(log: Logger): String {
    log.kotlinDebug("Loading version information")
    val props = Properties()
    val propFileName = "project.properties"
    val inputStream = javaClass.classLoader!!.getResourceAsStream(propFileName) ?:
            throw FileNotFoundException("property file '$propFileName' not found in the classpath")

    props.load(inputStream)

    val projectVersion = props["project.version"] as String
    log.kotlinDebug("Found project version [$projectVersion]")
    return projectVersion
}
