/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import com.github.gundy.semver4j.SemVer
import org.jetbrains.kotlin.gradle.targets.js.npm.FILE_VERSION_PREFIX
import java.io.File

internal class YarnEntryRegistry(private val lockFile: File) {
    private val entryMap = YarnLock.parse(lockFile)
        .entries
        .associateBy { it.dependencyKey }

    fun find(packageKey: String, version: String): YarnLock.Entry {
        val key = dependencyKey(packageKey, version)
        var entry = entryMap[key]

        if (entry == null && version == "*") {
            val searchKey = dependencyKey(packageKey, "")
            entry = entryMap.entries
                .filter { it.key.startsWith(searchKey) }
                .firstOrNull {
                    SemVer.satisfies(it.key.removePrefix(searchKey), "*")
                }
                ?.value
        }

        return checkNotNull(entry) {
            "Cannot find $key in yarn.lock"
        }
    }

    private val YarnLock.Entry.dependencyKey: String
        get() = key.correctDependencyKey()

    private fun dependencyKey(packageKey: String, version: String) =
        YarnLock.dependencyKey(packageKey, version).correctDependencyKey()

    private fun String.correctDependencyKey(): String =
        when {
            GITHUB_MARKER in this -> replace(GITHUB_MARKER, SEPARATOR)
            FILE_MARKER in this -> {
                val location = substringAfter(FILE_MARKER)
                val path = lockFile
                    .parentFile
                    .resolve(location)
                    .canonicalPath

                replaceAfter(FILE_MARKER, path)
            }
            else -> this
        }
}

private const val SEPARATOR = "@"

private const val FILE_MARKER = "${SEPARATOR}$FILE_VERSION_PREFIX"
private const val GITHUB_MARKER = "${SEPARATOR}github:"