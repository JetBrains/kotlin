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

package org.jetbrains.kotlin.jps.incremental.storages

import com.intellij.openapi.util.io.FileUtil

class PathFunctionPair(
    val path: String,
    val function: String
) : Comparable<PathFunctionPair> {
    override fun compareTo(other: PathFunctionPair): Int {
        val pathComp = FileUtil.comparePaths(path, other.path)

        if (pathComp != 0) return pathComp

        return function.compareTo(other.function)
    }

    override fun equals(other: Any?): Boolean =
        when (other) {
            is PathFunctionPair ->
                FileUtil.pathsEqual(path, other.path) && function == other.function
            else ->
                false
        }

    override fun hashCode(): Int = 31 * FileUtil.pathHashCode(path) + function.hashCode()
}
