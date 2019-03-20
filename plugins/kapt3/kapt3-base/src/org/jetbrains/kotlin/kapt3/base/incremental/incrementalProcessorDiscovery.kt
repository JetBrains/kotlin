/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.incremental

import java.io.File
import java.io.InputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private val INCREMENTAL_DECLARED_TYPES =
    setOf(DeclaredProcType.AGGREGATING.name, DeclaredProcType.ISOLATING.name, DeclaredProcType.DYNAMIC.name)
private const val INCREMENTAL_ANNOTATION_FLAG = "META-INF/gradle/incremental.annotation.processors"

/** Checks the incremental annotation processor information for the annotation processor classpath. */
fun getIncrementalProcessorsFromClasspath(
    names: Set<String>, classpath: Iterable<File>
): Map<String, DeclaredProcType> {
    val finalValues = mutableMapOf<String, DeclaredProcType>()

    classpath.forEach { entry ->
        val fromEntry = processSingleClasspathEntry(entry)
        fromEntry.filter { names.contains(it.key) }.forEach { finalValues[it.key] = it.value }

        if (finalValues.size == names.size) return finalValues
    }

    return finalValues
}

private fun processSingleClasspathEntry(rootFile: File): Map<String, DeclaredProcType> {
    val text: List<String> = when {
        rootFile.isDirectory -> {
            val markerFile = rootFile.resolve(INCREMENTAL_ANNOTATION_FLAG)
            if (markerFile.exists()) {
                markerFile.bufferedReader().readLines()
            } else {
                emptyList()
            }
        }
        rootFile.extension == "jar" -> ZipFile(rootFile).use { zipFile ->
            val content: InputStream? = zipFile.getInputStream(ZipEntry(INCREMENTAL_ANNOTATION_FLAG))

            content?.bufferedReader()?.readLines() ?: emptyList()
        }
        else -> emptyList()
    }

    val nameToType = mutableMapOf<String, DeclaredProcType>()
    for (line in text) {
        val parts = line.split(",")
        if (parts.size == 2) {
            val kind = parts[1].toUpperCase(Locale.ENGLISH)
            if (INCREMENTAL_DECLARED_TYPES.contains(kind)) {
                nameToType[parts[0]] = enumValueOf(kind)
            }
        }
    }
    return nameToType
}