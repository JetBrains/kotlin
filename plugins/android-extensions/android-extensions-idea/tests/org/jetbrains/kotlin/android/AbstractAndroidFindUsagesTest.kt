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

package org.jetbrains.kotlin.android


abstract class AbstractAndroidFindUsagesTest : KotlinAndroidTestCase() {

    fun doTest(path: String) {
        return // TODO: investigate and fix this test
        copyResourceDirectoryForTest(path)
        val testFileName = getTestName(true) + ".kt"
        val virtualFile = myFixture.copyFileToProject(path + testFileName, "src/" + getTestName(true) + ".kt")
        myFixture.configureFromExistingVirtualFile(virtualFile)

        val propUsages = myFixture.findUsages(myFixture.elementAtCaret)
        assertTrue(propUsages.isNotEmpty())
    }
}