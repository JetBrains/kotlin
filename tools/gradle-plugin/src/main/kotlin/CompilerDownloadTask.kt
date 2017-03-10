package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.konan.DependencyDownloader
import java.io.File

open class CompilerDownloadTask: DefaultTask() {

    internal companion object {
        internal const val DOWNLOAD_URL = "http://download.jetbrains.com/kotlin/native"

        internal val KONAN_NAME = "kotlin-native-${simpleOsName()}-${KonanPlugin.KONAN_VERSION}"
        private  val KONAN_PARENT_DIR = "${System.getProperty("user.home")}/.konan"

        internal fun simpleOsName(): String {
            val osName = System.getProperty("os.name")
            return when (osName) {
                "Mac OS X" -> "macos"
                "Linux" -> "linux"
                else -> throw IllegalStateException("Unsupported platform: $osName")
            }
        }
    }

    @TaskAction
    fun downloadAndExtract() {
        if (project.hasProperty(KonanPlugin.KONAN_HOME_PROPERTY_NAME)) {
            val konanHome = project.property(KonanPlugin.KONAN_HOME_PROPERTY_NAME)
            logger.info("Use a user-defined compiler path: $konanHome")
            return
        }
        try {
            logger.info("Downloading Kotlin Native compiler from $DOWNLOAD_URL into $KONAN_PARENT_DIR")
            DependencyDownloader(File(KONAN_PARENT_DIR), DOWNLOAD_URL, listOf(KONAN_NAME)).run()
            project.extensions.extraProperties.set(KonanPlugin.KONAN_HOME_PROPERTY_NAME, "$KONAN_PARENT_DIR/$KONAN_NAME")
        } catch (e: RuntimeException) {
            throw GradleScriptException("Cannot download Kotlin Native compiler", e)
        }
    }
}