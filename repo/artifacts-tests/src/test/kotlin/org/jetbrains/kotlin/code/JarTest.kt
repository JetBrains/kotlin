/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.name
import kotlin.test.fail

class JarTest {
    @RepositoryFileTest("**/*.{zip,jar}")
    fun `test - no duplicate zip entries`(jar: Path) {
        val entries = hashSetOf<String>()
        val duplicates = hashSetOf<String>()
        ZipFile(jar.toFile()).use { zipFile ->
            zipFile.entries().asSequence().forEach { entry ->
                if (entries.add(entry.name)) return@forEach
                duplicates.add(entry.name)
            }
        }

        if (duplicates.isNotEmpty()) {
            fail("Duplicated entries in ${jar.name}:\n${duplicates.joinToString("\n")}")
        }
    }
}
