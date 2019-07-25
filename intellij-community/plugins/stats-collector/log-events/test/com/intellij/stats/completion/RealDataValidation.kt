/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.stats.completion

import com.intellij.stats.validation.EventLine
import com.intellij.stats.validation.InputSessionValidator
import com.intellij.stats.validation.SessionValidationResult
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class RealDataValidation {

    private lateinit var separator: InputSessionValidator
    val sessionStatuses = hashMapOf<String, Boolean>()

    @Before
    fun setup() {
        sessionStatuses.clear()
        val result = object : SessionValidationResult {
            override fun addErrorSession(errorSession: List<EventLine>) {
                val sessionUid = errorSession.first().sessionUid ?: return
                sessionStatuses[sessionUid] = false
            }

            override fun addValidSession(validSession: List<EventLine>) {
                val sessionUid = validSession.first().sessionUid ?: return
                sessionStatuses[sessionUid] = true
            }
        }
        separator = InputSessionValidator(result)
    }

    @Test
    fun testRealData() {
        val file = file("real_data")
        val files = file.list().sortedBy { it.substringAfter("_").toInt() }

        val chunkFiles = files.map { "real_data/$it" }
        val totalLines = chunkFiles.map { file(it) }.map { it.reader().readLines() }.flatten()

        separator.validate(totalLines)
        val invalidSessions = sessionStatuses.count { !it.value }

        val validSessions = sessionStatuses.count { it.value }

        Assert.assertEquals(7, validSessions)
        Assert.assertEquals(6, invalidSessions)
    }

    private fun file(path: String): File {
        return File(javaClass.classLoader.getResource(path).file)
    }

}