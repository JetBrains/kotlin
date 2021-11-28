/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap

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
