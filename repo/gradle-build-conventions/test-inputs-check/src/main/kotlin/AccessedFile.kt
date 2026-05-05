/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import jdk.jfr.consumer.RecordedFrame
import java.nio.file.Path

data class AccessedFile(
    val path: Path,
    val stacktrace: List<RecordedFrame>,
) {
    fun mapPath(mapper: (Path) -> Path) =
        AccessedFile(
            path = mapper(path),
            stacktrace = stacktrace
        )

    fun formatStacktrace() = buildString {
        stacktrace.forEach { frame ->
            val method = frame.method
            val className = method.type.name
            val methodName = method.name
            val lineNumber = frame.lineNumber
            append("    at $className.$methodName(")
            if (lineNumber >= 0) {
                append("${method.type.name.substringAfterLast('.')}.java:$lineNumber")
            } else {
                append("Unknown Source")
            }
            appendLine(")")
        }
    }

    override fun toString() = buildString {
        appendLine("path = $path")
        append(formatStacktrace())
    }
}
