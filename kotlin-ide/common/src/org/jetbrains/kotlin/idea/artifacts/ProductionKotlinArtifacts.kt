package org.jetbrains.kotlin.idea.artifacts

import com.intellij.util.PathUtil
import java.io.File

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
    override val kotlinStdlibSources = findFile(kotlincDirectory, KotlinArtifactNames.KOTLIN_STDLIB_SOURCES)
    override val kotlinReflect = findFile(kotlincDirectory, KotlinArtifactNames.KOTLIN_REFLECT)
    override val kotlinStdlibJs = findFile(kotlincDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS)
    override val kotlinTest = findFile(kotlincDirectory, KotlinArtifactNames.KOTLIN_TEST)
    override val kotlinMainKts = findFile(kotlincDirectory, KotlinArtifactNames.KOTLIN_MAIN_KTS)
    override val kotlinScriptRuntime = findFile(kotlincDirectory, KotlinArtifactNames.KOTLIN_SCRIPT_RUNTIME)
}