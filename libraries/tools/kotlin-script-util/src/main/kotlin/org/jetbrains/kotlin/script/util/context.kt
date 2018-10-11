package org.jetbrains.kotlin.script.util

import java.io.File
import kotlin.reflect.KClass
import kotlin.script.experimental.jvm.util.matchMaybeVersionedFile as newMatchMaybeVersionedFile
import kotlin.script.experimental.jvm.util.hasParentNamed as newHasParentName

@Deprecated("Use the function from kotlin.script.experimental.jvm.util")
fun classpathFromClassloader(classLoader: ClassLoader): List<File>? =
    kotlin.script.experimental.jvm.util.classpathFromClassloader(classLoader)

@Deprecated("Use the function from kotlin.script.experimental.jvm.util")
fun classpathFromClasspathProperty(): List<File>? =
    kotlin.script.experimental.jvm.util.classpathFromClasspathProperty()

@Deprecated("Use the function from kotlin.script.experimental.jvm.util")
fun classpathFromClass(classLoader: ClassLoader, klass: KClass<out Any>): List<File>? =
    kotlin.script.experimental.jvm.util.classpathFromClass(classLoader, klass)

@Deprecated("Use the function from kotlin.script.experimental.jvm.util")
fun classpathFromFQN(classLoader: ClassLoader, fqn: String): List<File>? =
    kotlin.script.experimental.jvm.util.classpathFromFQN(classLoader, fqn)

@Deprecated("Use the function from kotlin.script.experimental.jvm.util")
fun File.matchMaybeVersionedFile(baseName: String) = newMatchMaybeVersionedFile(baseName)

@Deprecated("Use the function from kotlin.script.experimental.jvm.util")
fun File.hasParentNamed(baseName: String): Boolean = newHasParentName(baseName)

@Deprecated("Use the function from kotlin.script.experimental.jvm.util")
fun scriptCompilationClasspathFromContextOrNull(
    vararg keyNames: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
): List<File>? =
    kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContextOrNull(
        *keyNames, classLoader = classLoader, wholeClasspath = wholeClasspath
    )

@Deprecated("Use the function from kotlin.script.experimental.jvm.util")
fun scriptCompilationClasspathFromContextOrStlib(
    vararg keyNames: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
): List<File> =
    kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContextOrStlib(
        *keyNames, classLoader = classLoader, wholeClasspath = wholeClasspath
    )

@Deprecated("Use the function from kotlin.script.experimental.jvm.util")
fun scriptCompilationClasspathFromContext(
    vararg keyNames: String,
    classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    wholeClasspath: Boolean = false
): List<File> =
    kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext(
        *keyNames, classLoader = classLoader, wholeClasspath = wholeClasspath
    )

@Deprecated("Use the object from kotlin.script.experimental.jvm.util")
val KotlinJars = kotlin.script.experimental.jvm.util.KotlinJars
