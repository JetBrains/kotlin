package org.jetbrains.kotlin.idea.artifacts

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.FileNotFoundException

abstract class KotlinArtifacts(kotlincDistDir: File) {
    companion object {
        @get:JvmStatic
        val instance: KotlinArtifacts by lazy {
            // ApplicationManager may absent in JPS process so we need to check it presence firstly
            if (doRunFromSources() && doesClassExist("com.intellij.openapi.application.ApplicationManager")) {
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

        private fun doRunFromSources(): Boolean {
            val resourcePathForClass = PathUtil.getResourcePathForClass(ProductionKotlinArtifacts::class.java)
            return resourcePathForClass.extension != "jar"
        }

        private fun doesClassExist(fqName: String): Boolean {
            val classPath = fqName.replace('.', '/') + ".class"
            return KotlinArtifacts::class.java.classLoader.getResource(classPath) != null
        }

        private fun getTestKotlinArtifacts(): KotlinArtifacts? {
            val clazz = try {
                Class.forName("org.jetbrains.kotlin.idea.artifacts.TestKotlinArtifacts")
            } catch (ex: ClassNotFoundException) {
                null
            }
            return clazz?.getConstructor()?.newInstance() as KotlinArtifacts?
        }
    }

    val kotlincDirectory = File(kotlincDistDir, "kotlinc")
    val kotlincLibDirectory = File(kotlincDirectory, "lib")

    val jetbrainsAnnotations = File(kotlincLibDirectory, KotlinArtifactNames.JETBRAINS_ANNOTATIONS)
    val kotlinStdlib = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB)
    val kotlinStdlibSources = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_SOURCES)
    val kotlinStdlibJdk7 = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK7)
    val kotlinStdlibJdk7Sources = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK7_SOURCES)
    val kotlinStdlibJdk8 = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK8)
    val kotlinStdlibJdk8Sources = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK8_SOURCES)
    val kotlinReflect = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_REFLECT)
    val kotlinStdlibJs = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS)
    val kotlinStdlibJsSources = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS_SOURCES)
    val kotlinTest = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST)
    val kotlinTestJunit = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST_JUNIT)
    val kotlinTestJs = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST_JS)
    val kotlinMainKts = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_MAIN_KTS)
    val kotlinScriptRuntime = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPT_RUNTIME)
    val kotlinScriptingCommon = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMMON)
    val kotlinScriptingJvm = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_JVM)
    val kotlinCompiler: File = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_COMPILER)
    val trove4j = File(kotlincLibDirectory, KotlinArtifactNames.TROVE4J)
    val kotlinDaemon = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_DAEMON)
    val kotlinScriptingCompiler = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMPILER)
    val kotlinScriptingCompilerImpl = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMPILER_IMPL)
    val kotlinCoroutinesExperimentalCompat = File(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_COROUTINES_EXPERIMENTAL_COMPAT)
    val allopenCompilerPlugin = File(kotlincLibDirectory, KotlinArtifactNames.ALLOPEN_COMPILER_PLUGIN)
    val noargCompilerPlugin = File(kotlincLibDirectory, KotlinArtifactNames.NOARG_COMPILER_PLUGIN)
    val samWithReceiverCompilerPlugin = File(kotlincLibDirectory, KotlinArtifactNames.SAM_WITH_RECEIVER_COMPILER_PLUGIN)
}

private object ProductionKotlinArtifacts : KotlinArtifacts(run {
    val pluginJar = PathUtil.getResourcePathForClass(ProductionKotlinArtifacts::class.java)
    if (!pluginJar.exists()) {
        throw IllegalStateException("Plugin JAR not found for class ${ProductionKotlinArtifacts::class.java}")
    }

    val libFile = pluginJar.parentFile.takeIf { it.name == "lib" }
    if (libFile == null || !libFile.exists()) {
        // Don't throw exception because someone may want to just try to initialize
        // KotlinArtifacts but won't actually use it. E.g. KotlinPluginMacros does it
        File("<invalid_kotlinc_path>")
    } else {
        libFile.parentFile
    }
})
