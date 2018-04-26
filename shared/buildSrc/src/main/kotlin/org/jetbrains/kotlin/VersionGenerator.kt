package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.*
import java.io.File


open class VersionGenerator: DefaultTask() {
    @OutputDirectory
    val versionSourceDirectory = project.file("build/generated")
    @OutputFile
    val versionFile:File = project.file("${versionSourceDirectory.path}/org/jetbrains/kotlin/konan/KonanVersion.kt")

    val konanVersion: String
        @Input get() = project.properties["konanVersion"].toString()

    val buildNumber: String?
        // TeamCity passes all configuration parameters into a build script as project properties.
        // Thus we can use them here instead of environment variables.
        @Optional @Input get() = project.findProperty("build.number")?.toString()

    val meta: String
        @Input get() = project.properties["konanMetaVersion"]?.let {
            "MetaVersion.${it.toString().toUpperCase()}"
        } ?: "MetaVersion.DEV"


    override fun configure(closure: Closure<*>): Task {
        val result = super.configure(closure)
        doFirst {
            val content = buildString {
                operator fun String.unaryPlus() = this@buildString.append(this)
                val version = konanVersion.split(".")
                val major = version[0].toInt()
                val minor = version[1].toInt()
                val maintenance = if (version.size > 2) version[2].toInt() else 0
                project.logger.info("BUILD_NUMBER: $buildNumber")
                val build = buildNumber?.let {
                    it.split("-")[2].toInt() //7-dev-buildcount
                }?: -1

                + """
                     |package org.jetbrains.kotlin.konan
                     |data class KonanVersion(val meta: MetaVersion = MetaVersion.DEV,
                     |                   val major: Int,
                     |                   val minor: Int,
                     |                   val maintenance: Int,
                     |                   val build:Int) {
                     |  companion object {
                     |     val CURRENT = KonanVersion($meta, $major, $minor, $maintenance, $build)
                     |  }
                     |  private fun versionToString(showMeta: Boolean = true, showBuild: Boolean = true) = buildString {
                     |    append(major)
                     |    append('.')
                     |    append(minor)
                     |    if (maintenance != 0) {
                     |      append('.')
                     |      append(maintenance)
                     |    }
                     |    if (showMeta) {
                     |      append('-')
                     |      append(meta.metaString)
                     |    }
                     |    if (showBuild && build != -1) {
                     |      append('-')
                     |      append(build)
                     |    }
                     |  }
                     |
                     |  private val isRelease: Boolean
                     |    get() = meta == MetaVersion.RELEASE
                     |
                     |  private val versionString by lazy { versionToString(!isRelease, !isRelease) }
                     |
                     |  override fun toString() = versionString
                     |
                     |  val gradlePluginVersion by lazy { versionString }
                     |}
                """.trimMargin()
            }
            versionFile.printWriter().use {
                it.println(content)
            }
        }
        return result
    }
}
