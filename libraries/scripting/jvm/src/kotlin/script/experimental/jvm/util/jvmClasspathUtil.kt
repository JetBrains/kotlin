/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.util

import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KClass
import kotlin.script.experimental.jvm.impl.getResourcePathForClass
import kotlin.script.experimental.jvm.impl.toFile
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
        val urls = (it as? URLClassLoader)?.urLs?.asList()
            ?: try {
                // e.g. for IDEA platform UrlClassLoader
                val getUrls = it::class.java.getMethod("getUrls")
                getUrls.isAccessible = true
                val result = getUrls.invoke(it) as? List<Any?>
                result?.filterIsInstance<URL>()
            } catch (e: Throwable) {
                null
            }
        urls?.mapNotNull {
            // taking only classpath elements pointing to dirs (presumably with classes) or jars, because this classpath is intended for
            //   usage with the kotlin compiler, which cannot process other types of entries, e.g. jni libs
            it.toFile()?.takeIf { el -> el.isDirectory || validClasspathFilesExtensions.any { el.extension == it } }
        }
            ?: emptyList()
    }.distinct().takeIf { it.isNotEmpty() }

fun classpathFromClasspathProperty(): List<File>? =
        System.getProperty("java.class.path")
                ?.split(String.format("\\%s", File.pathSeparatorChar).toRegex())
                ?.dropLastWhile(String::isEmpty)
                ?.map(::File)

fun classpathFromClass(classLoader: ClassLoader, klass: KClass<out Any>): List<File>? =
    classpathFromFQN(classLoader, klass.qualifiedName!!)

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

fun File.hasParentNamed(baseName: String): Boolean =
    nameWithoutExtension == baseName || parentFile?.hasParentNamed(baseName) ?: false

private const val KOTLIN_COMPILER_EMBEDDABLE_JAR = "$KOTLIN_COMPILER_NAME-embeddable.jar"

internal fun List<File>.takeIfContainsAll(vararg keyNames: String): List<File>? =
        takeIf { classpath ->
            keyNames.all { key -> classpath.any { it.matchMaybeVersionedFile(key) } }
        }

internal fun List<File>.filterIfContainsAll(vararg keyNames: String): List<File>? {
    val res = hashMapOf<String, File>()
    for (cpentry in this) {
        for (prefix in keyNames) {
            if (cpentry.matchMaybeVersionedFile(prefix) ||
                (cpentry.isDirectory && cpentry.hasParentNamed(prefix))
            ) {
                res[prefix] = cpentry
                break
            }
        }
    }
    return if (keyNames.all { res.containsKey(it) }) res.values.toList()
    else null
}

internal fun List<File>.takeIfContainsAny(vararg keyNames: String): List<File>? =
        takeIf { classpath ->
            keyNames.any { key -> classpath.any { it.matchMaybeVersionedFile(key) } }
        }

fun scriptCompilationClasspathFromContextOrNull(
    vararg keyNames: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
): List<File>? {
    fun List<File>.takeAndFilter() = when {
        isEmpty() -> null
        wholeClasspath -> takeIfContainsAll(*keyNames)
        else -> filterIfContainsAll(*keyNames)
    }
    return System.getProperty(KOTLIN_SCRIPT_CLASSPATH_PROPERTY)?.split(File.pathSeparator)?.map(::File)
        ?: classpathFromClassloader(classLoader)?.takeAndFilter()
        ?: classpathFromClasspathProperty()?.takeAndFilter()
}

fun scriptCompilationClasspathFromContextOrStlib(
    vararg keyNames: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
): List<File> =
    scriptCompilationClasspathFromContextOrNull(
        *keyNames,
        classLoader = classLoader,
        wholeClasspath = wholeClasspath
    )
            ?: KotlinJars.kotlinScriptStandardJars

fun scriptCompilationClasspathFromContext(
    vararg keyNames: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
): List<File> =
    scriptCompilationClasspathFromContextOrNull(
        *keyNames,
        classLoader = classLoader,
        wholeClasspath = wholeClasspath
    )
            ?: throw Exception("Unable to get script compilation classpath from context, please specify explicit classpath via \"$KOTLIN_SCRIPT_CLASSPATH_PROPERTY\" property")

object KotlinJars {

    private val explicitCompilerClasspath: List<File>? by lazy {
        System.getProperty(KOTLIN_COMPILER_CLASSPATH_PROPERTY)?.split(File.pathSeparator)?.map(::File)
        ?: System.getProperty(KOTLIN_COMPILER_JAR_PROPERTY)?.let(::File)?.takeIf(File::exists)?.let { listOf(it) }
    }

    val compilerClasspath: List<File> by lazy {
        val kotlinCompilerJars = listOf(
            KOTLIN_COMPILER_JAR,
            KOTLIN_COMPILER_EMBEDDABLE_JAR
        )
        val kotlinLibsJars = listOf(
            KOTLIN_JAVA_STDLIB_JAR,
            KOTLIN_JAVA_REFLECT_JAR,
            KOTLIN_JAVA_SCRIPT_RUNTIME_JAR
        )
        val kotlinBaseJars = kotlinCompilerJars + kotlinLibsJars

        val classpath = explicitCompilerClasspath
                        // search classpath from context classloader and `java.class.path` property
                        ?: (classpathFromFQN(
                            Thread.currentThread().contextClassLoader,
                            "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
                        )
                            ?: classpathFromClassloader(Thread.currentThread().contextClassLoader)?.takeIf { it.isNotEmpty() }
                            ?: classpathFromClasspathProperty()
                           )?.filter { f -> kotlinBaseJars.any { f.matchMaybeVersionedFile(it) } }?.takeIf { it.isNotEmpty() }
        // if autodetected, additionaly check for presense of the compiler jars
        if (classpath == null || (explicitCompilerClasspath == null && classpath.none { f -> kotlinCompilerJars.any { f.matchMaybeVersionedFile(it) } })) {
            throw FileNotFoundException("Cannot find kotlin compiler jar, set kotlin.compiler.classpath property to proper location")
        }
        classpath!!
    }

    fun getLib(propertyName: String, jarName: String, markerClass: KClass<*>): File? =
            System.getProperty(propertyName)?.let(::File)?.takeIf(File::exists)
            ?: explicitCompilerClasspath?.firstOrNull { it.matchMaybeVersionedFile(jarName) }?.takeIf(File::exists)
            ?: getResourcePathForClass(markerClass.java).takeIf(File::exists)

    val stdlibOrNull: File? by lazy {
        System.getProperty(KOTLIN_STDLIB_JAR_PROPERTY)?.let(::File)?.takeIf(File::exists)
                ?: getLib(
                    KOTLIN_RUNTIME_JAR_PROPERTY,
                    KOTLIN_JAVA_STDLIB_JAR,
                    JvmStatic::class
                )
    }

    val stdlib: File by lazy {
        stdlibOrNull
                ?: throw Exception("Unable to find kotlin stdlib, please specify it explicitly via \"$KOTLIN_STDLIB_JAR_PROPERTY\" property")
    }

    val scriptRuntimeOrNull: File? by lazy {
        getLib(
            KOTLIN_SCRIPT_RUNTIME_JAR_PROPERTY,
            KOTLIN_JAVA_SCRIPT_RUNTIME_JAR,
            ScriptTemplateWithArgs::class
        )
    }

    val scriptRuntime: File by lazy {
        scriptRuntimeOrNull
                ?: throw Exception("Unable to find kotlin script runtime, please specify it explicitly via \"$KOTLIN_SCRIPT_RUNTIME_JAR_PROPERTY\" property")
    }

    val kotlinScriptStandardJars get() = listOf(
        stdlibOrNull,
        scriptRuntimeOrNull
    ).filterNotNull()
}
