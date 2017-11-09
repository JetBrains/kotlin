/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.idea.refactoring.introduce.ExtractTestFiles
import org.jetbrains.kotlin.idea.refactoring.introduce.checkExtract
import org.jetbrains.kotlin.idea.refactoring.introduce.doExtractFunction
import org.jetbrains.kotlin.psi.KtFile

abstract class AbstractAndroidExtractionTest: KotlinAndroidTestCase() {
    fun doTest(path: String) {
        copyResourceDirectoryForTest(path)
        val testFilePath = path + getTestName(true) + ".kt"
        val virtualFile = myFixture.copyFileToProject(testFilePath, "src/" + getTestName(true) + ".kt")
        myFixture.configureFromExistingVirtualFile(virtualFile)

        checkExtract(ExtractTestFiles(testFilePath, myFixture.file)) { file ->
            doExtractFunction(myFixture, file as KtFile)
        }
    }
}