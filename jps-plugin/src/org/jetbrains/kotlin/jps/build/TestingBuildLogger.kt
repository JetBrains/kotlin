/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleLevelBuilder
import org.jetbrains.kotlin.incremental.storage.version.CacheAttributesDiff
import org.jetbrains.kotlin.jps.targets.KotlinModuleBuildTarget
import java.io.File

/**
 * Used for assertions in tests.
 */
interface TestingBuildLogger {
    fun invalidOrUnusedCache(chunk: KotlinChunk?, target: KotlinModuleBuildTarget<*>?, attributesDiff: CacheAttributesDiff<*>)
    fun chunkBuildStarted(context: CompileContext, chunk: org.jetbrains.jps.ModuleChunk)
    fun afterChunkBuildStarted(context: CompileContext, chunk: org.jetbrains.jps.ModuleChunk)
    fun buildFinished(exitCode: ModuleLevelBuilder.ExitCode)
    fun markedAsDirtyBeforeRound(files: Iterable<File>)
    fun markedAsDirtyAfterRound(files: Iterable<File>)
}
