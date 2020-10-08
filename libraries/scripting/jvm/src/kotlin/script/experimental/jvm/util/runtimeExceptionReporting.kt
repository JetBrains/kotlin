/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.util

import java.io.PrintStream
import kotlin.script.experimental.api.ResultValue

fun ResultValue.Error.renderError(stream: PrintStream) {
    var trace = error.stackTrace
    val wrappingTrace = wrappingException?.stackTrace
    if (wrappingException == null || trace.size < wrappingTrace!!.size) {
        error.printStackTrace(stream)
    } else {
        // subtracting wrapping message stacktrace from error stacktrace to show only user-specific part of it

        fun PrintStream.printTrace(stackTrace: Array<StackTraceElement>, dropLastFrames: Int) {
            for (element in stackTrace.dropLast(dropLastFrames)) {
                println("\tat $element")
            }
        }

        stream.println(error)
        stream.printTrace(trace, wrappingTrace.size)

        var current: Throwable? = error.cause
        var wrapping = error
        val cyclesDetection = hashSetOf(wrapping)
        while (current != null && cyclesDetection.add(current)) {
            trace = current.stackTrace
            val sameFramesCount =
                trace.asList().asReversed().asSequence()
                    .zip(wrapping.stackTrace.asList().asReversed().asSequence())
                    .takeWhile { it.first == it.second }
                    .count()
            stream.println("Caused by: $current")
            stream.printTrace(trace, sameFramesCount)
            wrapping = current
            current = current.cause
        }
    }
}
