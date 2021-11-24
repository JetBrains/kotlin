/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.konan.blackboxtest.support.PackageFQN
import org.jetbrains.kotlin.renderer.KeywordStringsGenerated.KEYWORDS
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.math.min

internal fun computePackageName(testDataBaseDir: File, testDataFile: File): PackageFQN {
    assertTrue(testDataFile.startsWith(testDataBaseDir)) {
        """
            The file is outside of the directory.
            File: $testDataFile
            Directory: $testDataBaseDir
        """.trimIndent()
    }

    return testDataFile.parentFile
        .relativeTo(testDataBaseDir)
        .resolve(testDataFile.nameWithoutExtension)
        .toPath()
        .map(Path::name)
        .joinToString(".") { packagePart ->
            if (packagePart in KEYWORDS)
                "_${packagePart}_"
            else
                buildString {
                    packagePart.forEachIndexed { index, ch ->
                        if (ch.isJavaIdentifierPart()) {
                            if (index == 0 && !ch.isJavaIdentifierStart()) {
                                // If the first character is not suitable for start, escape it with '_'.
                                append('_')
                            }
                            append(ch)
                        } else {
                            // Replace incorrect character.
                            append('_')
                        }
                    }
                }
        }
}

internal fun Set<PackageFQN>.findCommonPackageName(): PackageFQN? = when (size) {
    0 -> null
    1 -> first()
    else -> map { packageName: PackageFQN ->
        packageName.split('.')
    }.reduce { commonPackageNameParts: List<String>, packageNameParts: List<String> ->
        ArrayList<String>(min(commonPackageNameParts.size, packageNameParts.size)).apply {
            val i = commonPackageNameParts.iterator()
            val j = packageNameParts.iterator()

            while (i.hasNext() && j.hasNext()) {
                val packageNamePart = i.next()
                if (packageNamePart == j.next()) add(packageNamePart) else break
            }
        }
    }.takeIf { it.isNotEmpty() }?.joinToString(".")
}

internal fun joinPackageNames(a: PackageFQN, b: PackageFQN): PackageFQN = when {
    a.isEmpty() -> b
    b.isEmpty() -> a
    else -> "$a.$b"
}

internal fun String.prependPackageName(packageName: PackageFQN): String =
    if (packageName.isEmpty()) this else "$packageName.$this"
