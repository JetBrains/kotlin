/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.FSOperations
import org.jetbrains.jps.incremental.fs.CompilationRound
import java.io.File

class FSOperationsHelper(
        private val compileContext: CompileContext,
        private val chunk: ModuleChunk,
        private val log: Logger
) {
    private var markedDirty = false

    fun hasMarkedDirty(): Boolean = markedDirty

    private val buildLogger = compileContext.testingContext?.buildLogger ?: BuildLogger.DO_NOTHING

    fun markChunk(recursively: Boolean, kotlinOnly: Boolean, excludeFiles: Set<File> = setOf()) {
        fun shouldMark(file: File): Boolean {
            if (kotlinOnly && !KotlinSourceFileCollector.isKotlinSourceFile(file)) return false

            if (file in excludeFiles) return false

            markedDirty = true
            return true
        }

        if (recursively) {
            FSOperations.markDirtyRecursively(compileContext, CompilationRound.NEXT, chunk, ::shouldMark)
        }
        else {
            FSOperations.markDirty(compileContext, CompilationRound.NEXT, chunk, ::shouldMark)
        }
    }

    fun markFiles(files: Iterable<File>, excludeFiles: Set<File> = setOf()) {
        val filesToMark = files.toMutableSet()
        filesToMark.removeAll(excludeFiles)

        log.debug("Mark dirty: $filesToMark")
        buildLogger.markedAsDirty(filesToMark)

        for (file in filesToMark) {
            if (!file.exists()) continue

            FSOperations.markDirty(compileContext, CompilationRound.NEXT, file)
        }

        markedDirty = markedDirty || filesToMark.isNotEmpty()
    }
}