/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.zip.CRC32
import kotlin.io.path.inputStream

private val checksumStringEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

internal fun Path.crc32Checksum(): Int {
    val crc32 = CRC32()
    val buffer = ByteArray(2048)
    this.inputStream().buffered().use { fileStream ->
        while (true) {
            val read = fileStream.read(buffer)
            if (read < 0) break
            crc32.update(buffer, 0, read)
        }
    }
    return crc32.value.toInt()
}

internal fun Path.crc32ChecksumString(): String {
    return checksumString(crc32Checksum())
}

internal fun checksumString(checksum: Int): String {
    return checksumString(ByteBuffer.allocate(4).putInt(checksum).array())
}

internal fun checksumString(checksum: ByteArray): String {
    return checksumStringEncoder.encodeToString(checksum)
}
