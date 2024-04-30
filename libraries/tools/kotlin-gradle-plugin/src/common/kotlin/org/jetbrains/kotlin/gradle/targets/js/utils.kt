/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.internal.hash.FileHasher
import org.gradle.internal.hash.Hashing.defaultFunction
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsSetupTask.Companion.CACHE_VERSION
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File
import java.nio.file.Files

fun Appendable.appendConfigsFromDir(confDir: File) {
    val files = confDir.listFiles() ?: return

    files.asSequence()
        .filter { it.isFile }
        .filter { it.extension == "js" }
        .sortedBy { it.name }
        .forEach {
            appendLine("// ${it.name}")
            append(it.readText())
            appendLine()
            appendLine()
        }
}

fun ByteArray.toHex(): String {
    val result = CharArray(size * 2) { ' ' }
    var i = 0
    forEach {
        val n = it.toInt()
        result[i++] = Character.forDigit(n shr 4 and 0xF, 16)
        result[i++] = Character.forDigit(n and 0xF, 16)
    }
    return String(result)
}

fun extractWithUpToDate(
    destination: File,
    destinationHashFile: File,
    dist: File,
    fileHasher: FileHasher,
    extract: (File, File) -> Unit,
) {
    var distHash: String? = null
    val upToDate = destinationHashFile.let { file ->
        if (file.exists()) {
            file.useLines { seq ->
                val list = seq.first().split(" ")
                list.size == 3 &&
                        list[0] == CACHE_VERSION &&
                        list[1] == fileHasher.calculateDirHash(destination) &&
                        list[2] == fileHasher.hash(dist).toByteArray().toHex().also { distHash = it }
            }
        } else false
    }

    if (upToDate) {
        return
    }

    if (destination.isDirectory) {
        destination.deleteRecursively()
    }

    extract(dist, destination.parentFile)

    destinationHashFile.writeText(
        CACHE_VERSION +
                " " +
                fileHasher.calculateDirHash(destination)!! +
                " " +
                (distHash ?: fileHasher.hash(dist).toByteArray().toHex())
    )
}

fun FileHasher.calculateDirHash(
    dir: File,
): String? {
    if (!dir.isDirectory) return null

    val hasher = defaultFunction().newHasher()
    dir.walk()
        .forEach { file ->
            hasher.putString(file.toRelativeString(dir))
            if (file.isFile) {
                if (!Files.isSymbolicLink(file.toPath())) {
                    hasher.putHash(hash(file))
                } else {
                    val canonicalFile = file.canonicalFile
                    hasher.putHash(hash(canonicalFile))
                    hasher.putString(canonicalFile.toRelativeString(dir))
                }
            }
        }

    return hasher.hash().toByteArray().toHex()
}

const val JS = "js"
const val JS_MAP = "js.map"
const val META_JS = "meta.js"
const val HTML = "html"

internal fun writeWasmUnitTestRunner(compiledFile: File): File {
    val testRunnerFile = compiledFile.parentFile.resolve("runUnitTests.mjs")
    testRunnerFile.writeText(
        """
        import exports from './${compiledFile.name}';
        exports.startUnitTests?.();
        """.trimIndent()
    )
    return testRunnerFile
}
