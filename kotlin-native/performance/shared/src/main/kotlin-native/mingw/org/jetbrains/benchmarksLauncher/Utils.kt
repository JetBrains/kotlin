/*
 * Copyright 2010-2023 JetBrains s.r.o.
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
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.benchmarksLauncher

import platform.posix.*
import platform.windows.*
import kotlinx.cinterop.*

actual fun currentTime() =
        memScoped {
            val timeVal = alloc<timeval>()
            mingw_gettimeofday(timeVal.ptr, null)
            val sec = alloc<LongVar>()
            sec.value = timeVal.tv_sec.convert()
            val nowtm = localtime(sec.ptr)
            var timeBuffer = ByteArray(1024)
            strftime(timeBuffer.refTo(0), timeBuffer.size.toULong(), "%H:%M:%S", nowtm)

            timeBuffer.toKString()
        }
