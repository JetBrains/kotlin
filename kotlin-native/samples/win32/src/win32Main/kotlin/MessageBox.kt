/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.win32

import kotlinx.cinterop.*
import platform.windows.*

fun main() {
    val message = StringBuilder()
    memScoped {
      val buffer = allocArray<UShortVar>(MAX_PATH)
      GetModuleFileNameW(null, buffer, MAX_PATH)
      val path = buffer.toKString().split("\\").dropLast(1).joinToString("\\")
      message.append("Я нахожусь в $path\n")
    }
    MessageBoxW(null, "Konan говорит:\nЗДРАВСТВУЙ МИР!\n$message",
            "Заголовок окна", (MB_YESNOCANCEL or MB_ICONQUESTION).convert())
}
