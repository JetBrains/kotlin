package org.jetbrains.kotlin.script.util

import org.jetbrains.kotlin.script.util.impl.getResourcePathForClass
import org.jetbrains.kotlin.script.util.impl.toFile
import java.io.File
import java.io.FileNotFoundException
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.templates.standard.ScriptTemplateWithArgs

// TODO: consider moving all these utilites to the build-common or some other shared compiler API module

internal const val KOTLIN_JAVA_STDLIB_JAR = "kotlin-stdlib.jar"
internal const val KOTLIN_JAVA_REFLECT_JAR = "kotlin-reflect.jar"
internal const val KOTLIN_JAVA_SCRIPT_RUNTIME_JAR = "kotlin-script-runtime.jar"
internal const val KOTLIN_COMPILER_NAME = "kotlin-compiler"
internal const val KOTLIN_COMPILER_JAR = "$KOTLIN_COMPILER_NAME.jar"

internal const val KOTLIN_SCRIPT_CLASSPATH_PROPERTY = "kotlin.script.classpath"
internal const val KOTLIN_COMPILER_CLASSPATH_PROPERTY = "kotlin.compiler.classpath"
internal const val KOTLIN_COMPILER_JAR_PROPERTY = "kotlin.compiler.jar"
internal const val KOTLIN_STDLIB_JAR_PROPERTY = "kotlin.java.stdlib.jar"
// obsolete name, but maybe still used in the wild
// TODO: consider removing
internal const val KOTLIN_RUNTIME_JAR_PROPERTY = "kotlin.java.runtime.jar"
internal const val KOTLIN_SCRIPT_RUNTIME_JAR_PROPERTY = "kotlin.script.runtime.jar"

private val validClasspathFilesExtensions = setOf("jar", "zip", "java")

fun classpathFromClassloader(classLoader: ClassLoader): List<File>? =
        generateSequence(classLoader) { it.parent }.toList().flatMap {
            (it as? URLClassLoader)?.urLs?.mapNotNull {
                // taking only classpath elements pointing to dirs (presumably with classes) or jars, because this classpath is intended for
                //   usage with the kotlin compiler, which cannot process other types of entries, e.g. jni libs
                it.toFile()?.takeIf { el -> el.isDirectory || validClasspathFilesExtensions.any { el.extension == it } }
            }
            ?: emptyList()
        }

fun classpathFromClasspathProperty(): List<File>? =
        System.getProperty("java.class.path")
                ?.split(String.format("\\%s", File.pathSeparatorChar).toRegex())
                ?.dropLastWhile(String::isEmpty)
                ?.map(::File)

fun classpathFromClass(classLoader: ClassLoader, klass: KClass<out Any>): List<File>? = classpathFromFQN(classLoader, klass.qualifiedName!!)

fun classpathFromFQN(classLoader: ClassLoader, fqn: String): List<File>? {
    val clp = "${fqn.replace('.', '/')}.class"
    val url = classLoader.getResource(clp)
    return url?.toURI()?.path?.removeSuffix(clp)?.let {
        listOf(File(it))
    }
}

fun File.matchMaybeVersionedFile(baseName: String) =
        name == baseName ||
        name == baseName.removeSuffix(".jar") || // for classes dirs
        Regex(Regex.escape(baseName.removeSuffix(".jar")) + "(-\\d.*)?\\.jar").matches(name)

private const val KOTLIN_COMPILER_EMBEDDABLE_JAR = "$KOTLIN_COMPILER_NAME-embeddable.jar"

internal fun List<File>.takeIfContainsAll(vararg keyNames: String): List<File>? =
        takeIf { classpath ->
            keyNames.all { key -> classpath.any { it.matchMaybeVersionedFile(key) } }
        }

internal fun List<File>.takeIfContainsAny(vararg keyNames: String): List<File>? =
        takeIf { classpath ->
            keyNames.any { key -> classpath.any { it.matchMaybeVersionedFile(key) } }
        }

fun scriptCompilationClasspathFromContext(vararg keyNames: String, classLoader: ClassLoader = Thread.currentThread().contextClassLoader): List<File> =
        System.getProperty(KOTLIN_SCRIPT_CLASSPATH_PROPERTY)?.split(File.pathSeparator)?.map(::File)
        ?: classpathFromClassloader(classLoader)?.takeIfContainsAll(*keyNames)
        ?: classpathFromClasspathProperty()?.takeIfContainsAll(*keyNames)
        ?: KotlinJars.kotlinScriptStandardJars

object KotlinJars {

    private val explicitCompilerClasspath: List<File>? by lazy {
        System.getProperty(KOTLIN_COMPILER_CLASSPATH_PROPERTY)?.split(File.pathSeparator)?.map(::File)
        ?: System.getProperty(KOTLIN_COMPILER_JAR_PROPERTY)?.let(::File)?.takeIf(File::exists)?.let { listOf(it) }
    }

    val compilerClasspath: List<File> by lazy {
        val kotlinCompilerJars = listOf(KOTLIN_COMPILER_JAR, KOTLIN_COMPILER_EMBEDDABLE_JAR)
        val kotlinLibsJars = listOf(KOTLIN_JAVA_STDLIB_JAR, KOTLIN_JAVA_REFLECT_JAR, KOTLIN_JAVA_SCRIPT_RUNTIME_JAR)
        val kotlinBaseJars = kotlinCompilerJars + kotlinLibsJars

        val classpath = explicitCompilerClasspath
                        // search classpath from context classloader and `java.class.path` property
                        ?: (classpathFromFQN(Thread.currentThread().contextClassLoader, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                            ?: classpathFromClassloader(Thread.currentThread().contextClassLoader)?.takeIf { it.isNotEmpty() }
                            ?: classpathFromClasspathProperty()
                           )?.filter { f -> kotlinBaseJars.any { f.matchMaybeVersionedFile(it) } }?.takeIf { it.isNotEmpty() }
        // if autodetected, additionaly check for presense of the compiler jars
        if (classpath == null || (explicitCompilerClasspath == null && classpath.none { f -> kotlinCompilerJars.any { f.matchMaybeVersionedFile(it) } })) {
            throw FileNotFoundException("Cannot find kotlin compiler jar, set kotlin.compiler.classpath property to proper location")
        }
        classpath!!
    }

    private fun getLib(propertyName: String, jarName: String, markerClass: KClass<*>): File? =
            System.getProperty(propertyName)?.let(::File)?.takeIf(File::exists)
            ?: explicitCompilerClasspath?.firstOrNull { it.matchMaybeVersionedFile(jarName) }?.takeIf(File::exists)
            ?: getResourcePathForClass(markerClass.java).takeIf(File::exists)

    val stdlib: File? by lazy {
        System.getProperty(KOTLIN_STDLIB_JAR_PROPERTY)?.let(::File)?.takeIf(File::exists)
        ?: getLib(KOTLIN_RUNTIME_JAR_PROPERTY, KOTLIN_JAVA_STDLIB_JAR, JvmStatic::class)
    }

    val scriptRuntime: File? by lazy {
        getLib(KOTLIN_SCRIPT_RUNTIME_JAR_PROPERTY, KOTLIN_JAVA_SCRIPT_RUNTIME_JAR, ScriptTemplateWithArgs::class)
    }

    val kotlinScriptStandardJars get() = listOf(stdlib, scriptRuntime).filterNotNull()
}
