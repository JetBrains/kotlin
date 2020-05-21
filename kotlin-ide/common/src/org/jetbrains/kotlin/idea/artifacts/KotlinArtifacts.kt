package org.jetbrains.kotlin.idea.artifacts

import com.intellij.util.PathUtil
import java.io.File

abstract class KotlinArtifacts {
    abstract val kotlincDirectory: File
    abstract val kotlinStdlib: File
    abstract val kotlinStdlibJs: File
    abstract val kotlinTest: File

    protected fun findFile(parent: File, path: String): File {
        val result = File(parent, path)
        if (!result.exists()) {
            throw IllegalStateException("File $result doesn't exist")
        }
        return result
    }
}

private val kotlinPluginDirectory: File = run {
    val pluginJar = File(PathUtil.getJarPathForClass(ProductionKotlinArtifacts::class.java))
    if (!pluginJar.exists()) {
        throw IllegalStateException("Plugin JAR not found for class ${ProductionKotlinArtifacts::class.java}")
    }

    val libFile = pluginJar.parentFile.takeIf { it.name == "lib" }
    if (libFile == null || !libFile.exists()) {
        throw IllegalStateException("'lib' plugin directory not found")
    }

    libFile.parentFile
}

object ProductionKotlinArtifacts : KotlinArtifacts() {
    override val kotlincDirectory = findFile(kotlinPluginDirectory, "kotlinc")
    override val kotlinStdlib = findFile(kotlincDirectory, KotlinArtifactNames.KOTLIN_STDLIB)
    override val kotlinStdlibJs = findFile(kotlincDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS)
    override val kotlinTest = findFile(kotlincDirectory, KotlinArtifactNames.KOTLIN_TEST)
}