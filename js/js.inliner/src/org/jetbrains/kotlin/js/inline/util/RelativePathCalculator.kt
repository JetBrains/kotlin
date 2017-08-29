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

package org.jetbrains.kotlin.js.inline.util

import java.io.File

class RelativePathCalculator(baseDir: File) {
    private val baseDirPath = generateSequence(baseDir.canonicalFile) { it.parentFile }.toList().asReversed()

    fun calculateRelativePathTo(file: File): String? {
        val path = generateSequence(file.canonicalFile) { it.parentFile }.toList().asReversed()
        if (baseDirPath[0] != path[0]) return null

        val commonLength = baseDirPath.zip(path).takeWhile { (first, second) -> first == second }.size

        val sb = StringBuilder()
        for (i in commonLength until baseDirPath.size) {
            sb.append("../")
        }
        for (i in commonLength until path.size) {
            sb.append(path[i].name).append('/')
        }
        sb.setLength(sb.lastIndex)

        return sb.toString()
    }
}