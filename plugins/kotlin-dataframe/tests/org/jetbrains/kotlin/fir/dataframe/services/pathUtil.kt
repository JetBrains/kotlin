/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe.services

import java.io.File
import java.net.JarURLConnection
import java.net.URL

// Based on an implementation in com.intellij.openapi.application.PathManager.getResourceRoot

internal fun getResourceRoot(context: Class<*>, path: String): String? {
    var url: URL? = context.getResource(path)
    if (url == null) {
        url = ClassLoader.getSystemResource(path.substring(1))
    }
    return if (url != null) extractRoot(url, path) else null
}

private const val JAR_PROTOCOL = "jar"
private const val FILE_PROTOCOL = "file"
private const val JAR_SEPARATOR = "!/"
private const val SCHEME_SEPARATOR = "://"

private fun extractRoot(resourceURL: URL, resourcePath: String): String? {
    if (!resourcePath.startsWith('/') || resourcePath.startsWith('\\')) return null

    var resultPath: String? = null
    val protocol = resourceURL.protocol
    if (protocol == FILE_PROTOCOL) {
        val path = resourceURL.toFileOrNull()!!.path
        val testPath = path.replace('\\', '/')
        val testResourcePath = resourcePath.replace('\\', '/')
        if (testPath.endsWith(testResourcePath, ignoreCase = true)) {
            resultPath = path.substring(0, path.length - resourcePath.length)
        }
    } else if (protocol == JAR_PROTOCOL) {
        val paths = splitJarUrl(resourceURL.file)
        if (paths?.first != null) {
            resultPath = File(paths.first).canonicalPath
        }
    }

    return resultPath?.trimEnd(File.separatorChar)
}

private fun splitJarUrl(url: String): Pair<String, String>? {
    val pivot = url.indexOf(JAR_SEPARATOR).takeIf { it >= 0 } ?: return null

    val resourcePath = url.substring(pivot + 2)
    var jarPath = url.substring(0, pivot)

    if (jarPath.startsWith(JAR_PROTOCOL + ":")) {
        jarPath = jarPath.substring(JAR_PROTOCOL.length + 1)
    }

    if (jarPath.startsWith(FILE_PROTOCOL)) {
        try {
            jarPath = URL(jarPath).toFileOrNull()!!.path.replace('\\', '/')
        } catch (e: Exception) {
            jarPath = jarPath.substring(FILE_PROTOCOL.length)
            if (jarPath.startsWith(SCHEME_SEPARATOR)) {
                jarPath = jarPath.substring(SCHEME_SEPARATOR.length)
            } else if (jarPath.startsWith(':')) {
                jarPath = jarPath.substring(1)
            }
        }

    }
    return Pair(jarPath, resourcePath)
}

fun tryGetResourcePathForClass(aClass: Class<*>): File? {
    val path = "/" + aClass.name.replace('.', '/') + ".class"
    return getResourceRoot(aClass, path)?.let {
        File(it).absoluteFile
    }
}

fun getResourcePathForClass(aClass: Class<*>): File {
    return tryGetResourcePathForClass(aClass) ?: throw IllegalStateException("Resource for class: ${aClass.name} not found")
}

fun tryGetResourcePathForClassByName(name: String, classLoader: ClassLoader): File? =
    try {
        classLoader.loadClass(name)?.let(::tryGetResourcePathForClass)
    } catch (_: ClassNotFoundException) {
        null
    } catch (_: NoClassDefFoundError) {
        null
    }

internal fun URL.toFileOrNull() =
    try {
        File(toURI())
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: java.net.URISyntaxException) {
        null
    } ?: run {
        if (protocol != "file") null
        else File(file)
    }

internal fun URL.toContainingJarOrNull(): File? =
    if (protocol == "jar") {
        (openConnection() as? JarURLConnection)?.jarFileURL?.toFileOrNull()
    } else null
