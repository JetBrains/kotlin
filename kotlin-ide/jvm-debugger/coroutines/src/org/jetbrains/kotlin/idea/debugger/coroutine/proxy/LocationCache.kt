/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.jdi.GeneratedLocation
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class LocationCache(val context: DefaultExecutionContext) {
    fun createLocation(stackTraceElement: StackTraceElement): Location {
        val type = context.classesCache[stackTraceElement.className].firstOrNull()
        return createLocation(type, stackTraceElement.methodName, stackTraceElement.lineNumber)
    }

    fun createLocation(
        type: ReferenceType?,
        methodName: String,
        line: Int
    ): Location {
        if (type != null && line >= 0) {
            try {
                val location = type.locationsOfLine(null, null, line).stream()
                        .filter { l: Location -> l.method().name() == methodName }
                        .findFirst().orElse(null)
                if (location != null) {
                    return location
                }
            } catch (ignored: AbsentInformationException) {
            }
        }
        return GeneratedLocation(context.debugProcess, type, methodName, line)
    }
}