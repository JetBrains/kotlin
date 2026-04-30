/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.tests.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile
import kotlin.io.path.relativeTo
import kotlin.streams.toList

/**
 * Resolves a resource directory path to a [Path] using the classloader.
 */
fun resolveResourceDir(dirInResources: String): Path {
    val resourceUrl = BuildSession::class.java.classLoader.getResource(dirInResources)
        ?: throw IllegalStateException("Resource directory $dirInResources not found")
    return Paths.get(resourceUrl.toURI())
}

/**
 * Copies files from the resources to the session directory.
 * These files will be deleted on session close along with the session directory.
 *
 * If resources are duplicated, the last occurrence will be copied.
 */
fun copyResourcesToDir(dirInResources: String, targetDir: Path): List<Path> {
    val resourceUrl = BuildSession::class.java.classLoader.getResource(dirInResources)
        ?: throw IllegalStateException("Resource directory $dirInResources not found")

    return when (resourceUrl.protocol) {
        "file" -> {
            val directory = Paths.get(resourceUrl.toURI())
            Files.walk(directory)
                .filter { Files.isRegularFile(it) }
                .map { file ->
                    val relativePath = file.relativeTo(directory)
                    val targetFile = targetDir.resolve(relativePath)
                    targetFile.parent.toFile().mkdirs()
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                    targetFile
                }.toList()
        }
        "jar" -> {
            val jarPath = resourceUrl.path.substringBefore("!").removePrefix("file:")
            val jarFile = JarFile(jarPath)
            val entries = jarFile.entries()
            val result = mutableListOf<Path>()
            val dirPrefix = if (dirInResources.endsWith("/")) dirInResources else "$dirInResources/"
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name.substringAfter(dirPrefix)
                if (entry.name.startsWith(dirPrefix) && !entry.isDirectory) {
                    val targetFile = targetDir.resolve(name)
                    targetFile.parent.toFile().mkdirs()
                    jarFile.getInputStream(entry).use { input ->
                        Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING)
                    }
                    result.add(targetFile)
                }
            }
            result
        }
        else -> throw UnsupportedOperationException("Unsupported protocol ${resourceUrl.protocol}")
    }
}
