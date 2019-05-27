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
                   |
                   |import java.io.Serializable
                   |
                   |interface KonanVersion : Serializable {
                   |    val meta: MetaVersion
                   |    val major: Int
                   |    val minor: Int
                   |    val maintenance: Int
                   |    val build: Int
                   |
                   |    fun toString(showMeta: Boolean, showBuild: Boolean): String
                   |
                   |    companion object {
                   |        val CURRENT = KonanVersionImpl($meta, $major, $minor, $maintenance, $build)
                   |    }
                   |}
                   |
                   |data class KonanVersionImpl(
                   |        override val meta: MetaVersion = MetaVersion.DEV,
                   |        override val major: Int,
                   |        override val minor: Int,
                   |        override val maintenance: Int,
                   |        override val build: Int
                   |) : KonanVersion {
                   |
                   |    override fun toString(showMeta: Boolean, showBuild: Boolean) = buildString {
                   |        append(major)
                   |        append('.')
                   |        append(minor)
                   |        if (maintenance != 0) {
                   |            append('.')
                   |            append(maintenance)
                   |        }
                   |        if (showMeta) {
                   |            append('-')
                   |            append(meta.metaString)
                   |        }
                   |        if (showBuild && build != -1) {
                   |            append('-')
                   |            append(build)
                   |        }
                   |    }
                   |
                   |    private val isRelease: Boolean
                   |        get() = meta == MetaVersion.RELEASE
                   |
                   |    private val versionString by lazy { toString(!isRelease, !isRelease) }
                   |
                   |    override fun toString() = versionString
                   |}
                   |fun String.parseKonanVersion(): KonanVersion {
                   |    val (major, minor, maintenance, meta, build) =
                   |       Regex(""${'"'}([0-9]+)\.([0-9]+)(?:\.([0-9]+))?(?:-(\p{Alpha}\p{Alnum}*))?(?:-([0-9]+))?""${'"'})
                   |         .matchEntire(this)!!.destructured
                   |    return KonanVersionImpl(
                   |      MetaVersion.findAppropriate(meta),
                   |      major.toInt(),
                   |      minor.toInt(),
                   |      maintenance.toIntOrNull() ?: 0,
                   |      build.toIntOrNull() ?: -1)
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
