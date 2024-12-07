/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.file.File

internal fun resolveLibraries(staticLibraries: List<String>, libraryPaths: List<String>): List<String> {
    val result = mutableListOf<String>()
    staticLibraries.forEach { library ->
        
        val resolution = libraryPaths.map { "$it/$library" } 
                .find { File(it).exists }

        if (resolution != null) {
            result.add(resolution)
        } else {
            error("Could not find '$library' binary in neither of $libraryPaths")
        }
    }
    return result
}

internal fun argsToCompiler(staticLibraries: Array<String>, libraryPaths: Array<String>) = argsToCompiler(staticLibraries.toList(), libraryPaths.toList())

internal fun argsToCompiler(staticLibraries: List<String>, libraryPaths: List<String>) = 
    resolveLibraries(staticLibraries, libraryPaths)
        .map { it -> listOf("-include-binary", it) }
        .flatten()
        .toTypedArray()

