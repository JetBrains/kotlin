/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File
import java.util.HashSet

internal class TaskOutputsBackup(
    val outputs: FileCollection,
    val previousOutputs: Map<File, Array<Byte>> = outputs
        .flatMap {
            if (it.isDirectory) {
                it.walk().filter(File::isFile)
            } else {
                sequenceOf(it)
            }
        }
        .filter { it.exists() }
        .toSet()
        .keysToMap { it.readBytes().toTypedArray() }
) {

    fun restoreOutputs() {
        outputs.forEach {
            if (it.isDirectory) {
                it.deleteRecursively()
            } else if (it.isFile) {
                it.delete()
            }
        }

        val dirs = HashSet<File>()

        for ((file, bytes) in previousOutputs) {
            val dir = file.parentFile
            if (dirs.add(dir)) {
                dir.mkdirs()
            }
            file.writeBytes(bytes.toByteArray())
        }
    }
}
