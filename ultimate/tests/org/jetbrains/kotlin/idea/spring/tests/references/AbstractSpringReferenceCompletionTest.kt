/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.spring.tests.references

import org.jetbrains.kotlin.idea.completion.test.AbstractJvmBasicCompletionTest
import org.jetbrains.kotlin.idea.spring.tests.SpringTestFixtureExtension
import org.jetbrains.kotlin.idea.spring.tests.loadConfigByMainFilePath
import org.jetbrains.kotlin.idea.test.TestFixtureExtension

abstract class AbstractSpringReferenceCompletionTest : AbstractJvmBasicCompletionTest() {
    override fun setUpFixture(testPath: String) {
        TestFixtureExtension.loadFixture<SpringTestFixtureExtension>(myModule).loadConfigByMainFilePath(testPath, myFixture)
        super.setUpFixture(testPath)
    }

    override fun tearDownFixture() {
        TestFixtureExtension.unloadFixture<SpringTestFixtureExtension>()
        super.tearDownFixture()
    }
}