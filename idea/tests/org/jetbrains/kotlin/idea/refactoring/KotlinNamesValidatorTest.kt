/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.refactoring.KotlinNamesValidator
import org.junit.Assert

class KotlinNamesValidatorTest : LightCodeInsightFixtureTestCase() {
    val validator: NamesValidator = KotlinNamesValidator()

    private fun isKeyword(string: String) = validator.isKeyword(string, null)
    private fun isIdentifier(string: String) = validator.isIdentifier(string, null)

    override fun setUp() {
        super.setUp()
        myFixture.configureByFiles()
    }

    fun testKeywords() {
        Assert.assertTrue(isKeyword("val"))
        Assert.assertTrue(isKeyword("class"))
        Assert.assertTrue(isKeyword("fun"))

        Assert.assertFalse(isKeyword("constructor"))
        Assert.assertFalse(isKeyword("123"))
        Assert.assertFalse(isKeyword("a.c"))
        Assert.assertFalse(isKeyword("-"))
    }

    fun testIdentifiers() {
        Assert.assertTrue(isIdentifier("abc"))
        Assert.assertTrue(isIdentifier("q_q"))
        Assert.assertTrue(isIdentifier("constructor"))
        Assert.assertTrue(isIdentifier("`val`"))

        Assert.assertFalse(isIdentifier("val"))
        Assert.assertFalse(isIdentifier("class"))
        Assert.assertFalse(isIdentifier("fun"))

        Assert.assertFalse(isIdentifier("123"))
        Assert.assertFalse(isIdentifier("a.c"))
        Assert.assertFalse(isIdentifier("-"))
        Assert.assertFalse(isIdentifier("``"))
        Assert.assertFalse(isIdentifier(""))
        Assert.assertFalse(isIdentifier("  '"))
    }
}
