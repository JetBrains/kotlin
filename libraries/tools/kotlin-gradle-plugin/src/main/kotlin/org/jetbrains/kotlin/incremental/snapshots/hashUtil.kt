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

package org.jetbrains.kotlin.incremental.snapshots

import java.io.File
import java.security.MessageDigest

internal val File.md5: ByteArray
    get() {
        val messageDigest = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(4048)
        inputStream().use { input ->
            while (true) {
                val len = input.read(buffer)
                if (len < 0) {
                    break
                }
                messageDigest.update(buffer, 0, len)
            }
        }
        return messageDigest.digest()
    }
