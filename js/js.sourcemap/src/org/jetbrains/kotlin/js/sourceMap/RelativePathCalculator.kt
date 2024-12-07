/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.sourceMap

import java.io.File

class RelativePathCalculator(baseDir: File) {
    private fun File.getAllParents() = generateSequence(absoluteFile.normalize()) { it.parentFile }.toList().asReversed()

    private val baseDirPath = baseDir.getAllParents()

    fun calculateRelativePathTo(file: File): String? {
        val parents = file.getAllParents()
        if (baseDirPath[0] != parents[0]) return null

        val commonLength = baseDirPath.zip(parents).count { (first, second) -> first == second }

        val sb = StringBuilder()
        for (i in commonLength until baseDirPath.size) {
            sb.append("../")
        }
        for (i in commonLength until parents.size) {
            sb.append(parents[i].name).append('/')
        }
        sb.setLength(sb.lastIndex)

        return sb.toString()
    }
}
