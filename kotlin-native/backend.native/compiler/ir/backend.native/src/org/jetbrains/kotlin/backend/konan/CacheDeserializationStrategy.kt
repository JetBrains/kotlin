/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.name.FqName

object CacheDeserializationStrategy {
    object Nothing : CacheDeserializationStrategyBase() {
        override fun contains(filePath: String) = false
        override fun contains(fqName: FqName, fileName: String) = false
    }

    object WholeModule : CacheDeserializationStrategyBase() {
        override fun contains(filePath: String) = true
        override fun contains(fqName: FqName, fileName: String) = true
    }

    class SingleFile(val filePath: String, val fqName: String) : CacheDeserializationStrategyBase() {
        override fun contains(filePath: String) = filePath == this.filePath

        override fun contains(fqName: FqName, fileName: String) =
                fqName.asString() == this.fqName && File(filePath).name == fileName
    }

    class MultipleFiles(filePaths: List<String>, fqNames: List<String>) : CacheDeserializationStrategyBase() {
        private val filePaths = filePaths.toSet()

        private val fqNamesWithNames = fqNames.mapIndexed { i: Int, fqName: String -> Pair(fqName, File(filePaths[i]).name) }.toSet()

        override fun contains(filePath: String) = filePath in filePaths

        override fun contains(fqName: FqName, fileName: String) = Pair(fqName.asString(), fileName) in fqNamesWithNames
    }
}
