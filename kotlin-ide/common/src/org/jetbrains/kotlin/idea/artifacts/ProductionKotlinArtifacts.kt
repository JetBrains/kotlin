package org.jetbrains.kotlin.idea.artifacts

import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

abstract class ProductionLikeKotlinArtifacts : KotlinArtifacts() {
    protected abstract val kotlinPluginDirectory: File

    override val kotlincDirectory by lazy { findFile(kotlinPluginDirectory, "kotlinc") }
    override val kotlincLibDirectory by lazy { findFile(kotlincDirectory, "lib") }

    override val jetbrainsAnnotations by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.JETBRAINS_ANNOTATIONS) }
    override val kotlinStdlib by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB) }
    override val kotlinStdlibSources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_SOURCES) }
    override val kotlinStdlibJdk7 by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK7) }
    override val kotlinStdlibJdk7Sources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK7_SOURCES) }
    override val kotlinStdlibJdk8 by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK8) }
    override val kotlinStdlibJdk8Sources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK8_SOURCES) }
    override val kotlinReflect by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_REFLECT) }
    override val kotlinStdlibJs by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS) }
    override val kotlinStdlibJsSources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS_SOURCES) }
    override val kotlinTest by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST) }
    override val kotlinTestJunit by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST_JUNIT) }
    override val kotlinTestJs by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST_JS) }
    override val kotlinMainKts by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_MAIN_KTS) }
    override val kotlinScriptRuntime by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPT_RUNTIME) }
    override val kotlinScriptingCommon by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMMON) }
    override val kotlinScriptingJvm by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_JVM) }
    override val kotlinCompiler: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_COMPILER) }
    override val trove4j: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.TROVE4J) }
    override val kotlinDaemon: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_DAEMON) }
    override val kotlinScriptingCompiler: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMPILER) }
    override val kotlinScriptingCompilerImpl: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMPILER_IMPL) }

    override val kotlinStdlibCommon get() = throw error("'stdlib-common' artifact is not available")
    override val kotlinStdlibCommonSources get() = throw error("'stdlib-common' artifact is not available")
}

object ProductionKotlinArtifacts : ProductionLikeKotlinArtifacts() {
    override val kotlinPluginDirectory: File = run {
        val pluginJar = PathUtil.getResourcePathForClass(ProductionKotlinArtifacts::class.java)
        if (!pluginJar.exists()) {
            throw IllegalStateException("Plugin JAR not found for class ${ProductionKotlinArtifacts::class.java}")
        }

        val libFile = pluginJar.parentFile.takeIf { it.name == "lib" }
        if (libFile == null || !libFile.exists()) {
            throw IllegalStateException("'lib' plugin directory not found")
        }

        libFile.parentFile
    }
}
