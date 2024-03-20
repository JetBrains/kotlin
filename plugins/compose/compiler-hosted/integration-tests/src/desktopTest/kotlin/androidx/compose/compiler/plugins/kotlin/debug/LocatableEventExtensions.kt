/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.debug

import com.sun.jdi.Location
import com.sun.jdi.event.LocatableEvent
import junit.framework.TestCase

fun List<LocatableEvent>.assertTrace(expected: String) {
    val actual = compressRunsWithoutLinenumber(this)
        .filter { (!it.location().method().isSynthetic) }
        .map { it.location().formatAsExpectation() }

    expected.lines().forEachIndexed { index, expectedLine ->
        TestCase.assertEquals(expectedLine, actual[index])
    }
}

/*
   Compresses runs of the same, linenumber-less location in the log:
   specifically removes locations without linenumber, that would otherwise
   print as byte offsets. This avoids overspecifying code generation
   strategy in debug tests.
 */
fun compressRunsWithoutLinenumber(
    loggedItems: List<LocatableEvent>,
): List<LocatableEvent> {
    var current = ""
    return loggedItems.filter {
        val location = it.location()
        val result = location.lineNumber() != -1 || current != location.formatAsExpectation()
        if (result) current = location.formatAsExpectation()
        result
    }
}

private fun Location.formatAsExpectation(): String {
    val synthetic = if (method().isSynthetic) " (synthetic)" else ""
    return "${sourceName()}:${lineNumber()} ${method().name()}$synthetic"
}
