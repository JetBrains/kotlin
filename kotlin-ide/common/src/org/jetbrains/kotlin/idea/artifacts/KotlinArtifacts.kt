package org.jetbrains.kotlin.idea.artifacts

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.FileNotFoundException

abstract class KotlinArtifacts {
    companion object {
        @get:JvmStatic
        val instance: KotlinArtifacts by lazy {
            // ApplicationManager may absent in JPS process so we need to check it presence firstly
            if (doesClassExist("com.intellij.openapi.application.ApplicationManager")) {
                // This isUnitTestMode is for reliability in case when Application is already initialized. This check isn't mandatory
                if (ApplicationManager.getApplication()?.isUnitTestMode == true) {
                    return@lazy getTestKotlinArtifacts() ?: error("""
                        We are in unit test mode! TestKotlinArtifacts must be available in such mode. Probably class was renamed or broken classpath
                    """.trimIndent())
                }
            }

            // If TestKotlinArtifacts is presented in classpath then it must be test environment
            getTestKotlinArtifacts() ?: ProductionKotlinArtifacts
        }

        private fun doesClassExist(fqName: String): Boolean {
            val classPath = fqName.replace('.', '/') + ".class"
            return KotlinArtifacts::class.java.classLoader.getResource(classPath) != null
        }

        private fun getTestKotlinArtifacts(): KotlinArtifacts? {
            val clazz = try {
                Class.forName("org.jetbrains.kotlin.idea.artifacts.TestKotlinArtifacts")
            }
            catch (ex: ClassNotFoundException) {
                null
            }
            return clazz?.getConstructor()?.newInstance() as KotlinArtifacts?
        }
    }

    abstract val kotlincDistDir: File
    val kotlincDirectory by lazy { findFile(kotlincDistDir, "kotlinc") }
    val kotlincLibDirectory by lazy { findFile(kotlincDirectory, "lib") }

    val jetbrainsAnnotations by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.JETBRAINS_ANNOTATIONS) }
    val kotlinStdlib by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB) }
    val kotlinStdlibSources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_SOURCES) }
    val kotlinStdlibJdk7 by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK7) }
    val kotlinStdlibJdk7Sources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK7_SOURCES) }
    val kotlinStdlibJdk8 by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK8) }
    val kotlinStdlibJdk8Sources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK8_SOURCES) }
    val kotlinReflect by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_REFLECT) }
    val kotlinStdlibJs by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS) }
    val kotlinStdlibJsSources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS_SOURCES) }
    val kotlinTest by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST) }
    val kotlinTestJunit by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST_JUNIT) }
    val kotlinTestJs by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST_JS) }
    val kotlinMainKts by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_MAIN_KTS) }
    val kotlinScriptRuntime by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPT_RUNTIME) }
    val kotlinScriptingCommon by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMMON) }
    val kotlinScriptingJvm by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_JVM) }
    val kotlinCompiler: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_COMPILER) }
    val trove4j: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.TROVE4J) }
    val kotlinDaemon: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_DAEMON) }
    val kotlinScriptingCompiler: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMPILER) }
    val kotlinScriptingCompilerImpl: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMPILER_IMPL) }
    val kotlinCoroutinesExperimentalCompat: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_COROUTINES_EXPERIMENTAL_COMPAT) }
    val allopenCompilerPlugin: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.ALLOPEN_COMPILER_PLUGIN) }
    val noargCompilerPlugin: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.NOARG_COMPILER_PLUGIN) }
    val samWithReceiverCompilerPlugin: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.SAM_WITH_RECEIVER_COMPILER_PLUGIN) }

    private fun findFile(parent: File, path: String): File {
        val result = File(parent, path)
        if (!result.exists()) {
            throw FileNotFoundException("File $result doesn't exist")
        }
        return result
    }
}

private object ProductionKotlinArtifacts : KotlinArtifacts() {
    override val kotlincDistDir: File by lazy {
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
