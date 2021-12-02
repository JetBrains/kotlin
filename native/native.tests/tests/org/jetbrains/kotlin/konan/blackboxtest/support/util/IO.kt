/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.util

import org.jetbrains.kotlin.konan.blackboxtest.support.runner.ProcessOutput
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Read bytes from the given [InputStream] without blocking.
 *
 * Note: This function does not guarantee that the whole [InputStream] contents is read. It only
 * guarantees that the bytes currently available in [InputStream] are read and no I/O blocks happen.
 */
internal fun InputStream.readBytesNonBlocking(): ByteArray {
    val result = ByteArrayOutputStream()
    val buffer = ByteArray(128)

    while (true) {
        val availableBytes = available()
        if (availableBytes == 0) break

        val readBytes = read(buffer)
        if (readBytes == 0) break

        result.write(buffer, 0, readBytes)
    }

    return result.toByteArray()
}

internal fun Process.readOutput(outputFilter: TestOutputFilter, nonBlocking: Boolean): ProcessOutput {
    val stdOut = if (nonBlocking) inputStream.readBytesNonBlocking() else inputStream.readBytes()
    val stdErr = if (nonBlocking) errorStream.readBytesNonBlocking() else errorStream.readBytes()

    return ProcessOutput(
        stdOut = outputFilter.filter(stdOut.toString(Charsets.UTF_8)),
        stdErr = stdErr.toString(Charsets.UTF_8)
    )
}
