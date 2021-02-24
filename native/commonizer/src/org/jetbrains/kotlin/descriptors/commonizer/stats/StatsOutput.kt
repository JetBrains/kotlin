/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.stats

import java.io.Closeable
import java.io.File
import java.io.PrintWriter

interface StatsOutput : Closeable {
    interface StatsHeader {
        fun toList(): List<String>
    }

    interface StatsRow {
        fun toList(): List<String>
    }

    fun writeHeader(header: StatsHeader)
    fun writeRow(row: StatsRow)
}

class FileStatsOutput(directory: File, baseName: String) : StatsOutput {
    init {
        directory.mkdirs()
    }

    private val writer: PrintWriter = directory.resolve("${baseName}_stats.csv").printWriter()
    private var width: Int = 0

    override fun writeHeader(header: StatsOutput.StatsHeader) {
        check(width == 0)
        val headerItems = header.toList()
        require(headerItems.isNotEmpty())

        width = headerItems.size
        headerItems.joinTo(writer, separator = "|", postfix = "\n")
    }

    override fun writeRow(row: StatsOutput.StatsRow) {
        check(width > 0)
        val rowItems = row.toList()
        require(rowItems.size == width)

        rowItems.joinTo(writer, separator = "|", postfix = "\n")
    }

    override fun close() {
        writer.close()
    }
}
