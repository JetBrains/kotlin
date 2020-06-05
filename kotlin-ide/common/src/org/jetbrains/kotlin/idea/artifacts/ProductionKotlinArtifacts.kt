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
    override val kotlincLibDirectory = findFile(kotlincDirectory, "lib")

    override val jetbrainsAnnotations = findFile(kotlincLibDirectory, KotlinArtifactNames.JETBRAINS_ANNOTATIONS)
    override val kotlinStdlib = findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB)
    override val kotlinStdlibSources = findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_SOURCES)
    override val kotlinReflect = findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_REFLECT)
    override val kotlinStdlibJs = findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS)
    override val kotlinStdlibJsSources = findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS_SOURCES)
    override val kotlinTest = findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST)
    override val kotlinMainKts = findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_MAIN_KTS)
    override val kotlinScriptRuntime = findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPT_RUNTIME)
}