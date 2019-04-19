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

class ValidatorTest {
    private companion object {
        const val SESSION_ID = "d09b94c2c1aa"
    }

    lateinit var validator: InputSessionValidator
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
        validator = InputSessionValidator(result)
    }

    private fun file(path: String): File {
        return File(javaClass.classLoader.getResource(path).file)
    }

    private fun doTest(fileName: String, isValid: Boolean) {
        val file = file("data/$fileName")
        validator.validate(file.readLines())
        Assert.assertEquals(isValid, sessionStatuses[SESSION_ID])
    }

    @Test
    fun testValidData() = doTest("valid_data.txt", true)

    @Test
    fun testDataWithAbsentFieldInvalid() = doTest("absent_field.txt", false)

    @Test
    fun testInvalidWithoutBacket() = doTest("no_bucket.txt", false)

    @Test
    fun testInvalidWithoutVersion() = doTest("no_version.txt", false)

    @Test
    fun testDataWithExtraFieldInvalid() = doTest("extra_field.txt", false)

    @Test
    fun testWrongFactorsDiff() = doTest("wrong_factors_diff.txt", false)

}