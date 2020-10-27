/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.tetris

import platform.posix.*
import kotlinx.cinterop.*

object Config {
    var width: Int = 10
        private set
    var height: Int = 20
        private set
    var startLevel = 0
        private set

    init {
        val file = fopen("config.txt", "r")
        if (file != null) {
            try {
                val buffer = ByteArray(2 * 1024)
                while (true) {
                    val nextLine = fgets(buffer.refTo(0), buffer.size, file)?.toKString()
                    if (nextLine == null || nextLine.isEmpty()) break
                    val records = nextLine.split('=')
                    if (records.size != 2) continue
                    val key = records[0].trim()
                    val value = records[1].trim()
                    when (key) {
                        "width" -> width = value.toInt()
                        "height" -> height = value.toInt()
                        "startLevel" -> startLevel = value.toInt()
                    }
                }
            } finally {
                fclose(file)
            }
        }
    }
}