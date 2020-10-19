/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import org.gradle.api.internal.file.IdentityFileResolver
import org.gradle.groovy.scripts.TextResourceScriptSource
import org.gradle.internal.exceptions.LocationAwareException
import org.gradle.internal.resource.UriTextResource
import org.jetbrains.kotlin.idea.scripting.gradle.importing.parsePositionFromException
import org.junit.Test
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.RuntimeException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KotlinDslScriptModelTest {
    @Test
    fun testExceptionPositionParsing() {
        val file = File(createTempDir("kotlinDslTest"), "build.gradle.kts")
        val line = 10

        val mockScriptSource = TextResourceScriptSource(UriTextResource("build file", file, IdentityFileResolver()))
        val mockException = LocationAwareException(RuntimeException(), mockScriptSource, line)
        val stringWriter = StringWriter()
        mockException.printStackTrace(PrintWriter(stringWriter))

        val fromException = parsePositionFromException(stringWriter.toString())
        assertNotNull(fromException, "Position should be parsed")
        assertEquals(fromException.first, file.absolutePath, "Wrong file name parsed")
        assertEquals(fromException.second.line, line, "Wrong line number parsed")
    }
}