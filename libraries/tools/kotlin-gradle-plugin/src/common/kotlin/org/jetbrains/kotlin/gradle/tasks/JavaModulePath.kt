/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.util.GradleVersion
import java.io.File
import java.io.RandomAccessFile
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import java.util.jar.Manifest
import java.util.zip.ZipException
import java.util.zip.ZipFile

private const val MODULE_INFO_CLASS_FILE = "module-info.class"

// Unescaped dot as in Gradle's JavaModuleDetector, where it matches any character
private val MULTI_RELEASE_MODULE_INFO = Regex("META-INF/versions/\\d+/module-info.class")

// From this version on, Gradle's JavaModuleDetector reads jars with JarFile
private val GRADLE_JAR_FILE_DETECTOR = GradleVersion.version("9.5.0-milestone-7")

/**
 * Returns the classpath entries that Gradle's `JavaCompile` passes on `--module-path`, in classpath order.
 * Throws where Gradle's `JavaModuleDetector` throws, which fails `compileJava` too. From Gradle 9.5 that includes any archive
 * that isn't a zip file.
 */
internal fun inferJavaModulePath(
    classpath: List<File>,
    readsWithJarFile: Boolean = readsJarsWithJarFile(GradleVersion.current()),
): List<File> = classpath.filter { file ->
    try {
        isGradleJavaModule(file, readsWithJarFile)
    } catch (e: Exception) {
        throw GradleException("Can't read $file to check whether it's a Java module", e)
    }
}

/**
 * Returns whether Gradle's internal `JavaModuleDetector` reports [file] as a module. Gradle 9.5 and later read a jar with
 * `JarFile`, and earlier versions with `JarInputStream`, which finds only a leading manifest and the first of two.
 */
internal fun isGradleJavaModule(
    file: File,
    readsWithJarFile: Boolean = readsJarsWithJarFile(GradleVersion.current()),
): Boolean = when {
    !file.exists() -> false
    !file.isFile -> File(file, MODULE_INFO_CLASS_FILE).exists()
    !file.name.endsWith(".jar") -> false
    readsWithJarFile -> JarFile(file).use { jar -> isModuleJar(jar.manifest, jar.entries().asSequence().map { it.name }) }
    else -> file.inputStream().use { input ->
        JarInputStream(input).use { jar ->
            // JarInputStream inflates every entry it passes, so the names come from the central directory where it agrees
            isModuleJar(jar.manifest, centralDirectoryNames(file)?.asSequence() ?: generateSequence { jar.nextJarEntry }.map { it.name })
        }
    }
}

/**
 * Returns the entry names `JarInputStream` would pass, from the central directory: none when the file doesn't start with an
 * entry, as in a self-extracting jar, and `null` when there is no readable central directory.
 */
private fun centralDirectoryNames(file: File): List<String>? {
    val start = RandomAccessFile(file, "r").use { if (it.length() < 4) null else ByteArray(4).also(it::readFully) }
    if (!start.contentEquals(LOCAL_FILE_HEADER)) return emptyList()
    return try {
        ZipFile(file).use { zip -> zip.entries().asSequence().map { it.name }.toList() }
    } catch (_: ZipException) {
        null
    }
}

private val LOCAL_FILE_HEADER = byteArrayOf(0x50, 0x4b, 0x03, 0x04)

/**
 * Returns whether [freeArgs] can set the module path or the classpath, which makes an inferred module path stale, since kotlinc
 * uses the last value of each. Either can also be set in an argument file.
 */
internal fun setsJavaPaths(freeArgs: List<String>): Boolean =
    freeArgs.any { it.startsWith("@") || it.startsWith("-Xmodule-path") || it.startsWith("-classpath") || it == "-cp" }

internal fun readsJarsWithJarFile(gradleVersion: GradleVersion): Boolean = gradleVersion >= GRADLE_JAR_FILE_DETECTOR

private fun isModuleJar(manifest: Manifest?, entryNames: Sequence<String>): Boolean {
    val attributes = manifest?.mainAttributes
    if (attributes?.getValue("Automatic-Module-Name") != null) return true
    val isMultiRelease = java.lang.Boolean.parseBoolean(attributes?.getValue("Multi-Release"))
    return entryNames.any { it == MODULE_INFO_CLASS_FILE || isMultiRelease && MULTI_RELEASE_MODULE_INFO.matches(it) }
}

/**
 * Returns whether one of [Parameters.moduleInfoFiles] exists. Reading the files as a value source adds no task dependencies,
 * and the configuration cache checks them again on each build.
 */
internal abstract class JavaModuleSource : ValueSource<Boolean, JavaModuleSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val moduleInfoFiles: ConfigurableFileCollection
    }

    override fun obtain(): Boolean = parameters.moduleInfoFiles.files.any { it.exists() }
}
