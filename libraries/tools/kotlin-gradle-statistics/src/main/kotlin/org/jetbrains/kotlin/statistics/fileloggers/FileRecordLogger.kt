/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.statistics.fileloggers

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileRecordLogger(private val statisticsFolder: File, private val fileName: String) : IRecordLogger {
    private val profileFileNameSuffix = ".profile"
    private val metrics = ArrayList<String>()

    override fun append(s: String) {
        metrics.add(s)
    }

    override fun close() {
        try {
            statisticsFolder.mkdirs()
            var file = File(statisticsFolder, fileName + profileFileNameSuffix)
            var suffixIndex = 0
            while (!file.createNewFile()) {
                file = File(statisticsFolder, "${fileName}.${suffixIndex++}$profileFileNameSuffix")
            }
            FileOutputStream(file, true).bufferedWriter().use {
                for (value in metrics) {
                    it.appendLine(value)
                }
            }
        } catch (e: IOException) {
            NullRecordLogger()
        }
    }
}
