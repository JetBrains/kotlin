/**
 * Copyright 2010-2018 JetBrains s.r.o.
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
package tetris

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