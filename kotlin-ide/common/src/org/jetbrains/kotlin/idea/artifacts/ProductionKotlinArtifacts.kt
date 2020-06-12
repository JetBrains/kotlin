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
    override val kotlincDirectory by lazy { findFile(kotlinPluginDirectory, "kotlinc") }
    override val kotlincLibDirectory by lazy { findFile(kotlincDirectory, "lib") }

    override val jetbrainsAnnotations by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.JETBRAINS_ANNOTATIONS) }
    override val kotlinStdlib by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB) }
    override val kotlinStdlibSources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_SOURCES) }
    override val kotlinReflect by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_REFLECT) }
    override val kotlinStdlibJs by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS) }
    override val kotlinStdlibJsSources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS_SOURCES) }
    override val kotlinTest by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST) }
    override val kotlinMainKts by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_MAIN_KTS) }
    override val kotlinScriptRuntime by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPT_RUNTIME) }

    override val kotlinStdlibCommon get() = throw error("'stdlib-common' artifact is not available")
    override val kotlinStdlibCommonSources get() = throw error("'stdlib-common' artifact is not available")
}