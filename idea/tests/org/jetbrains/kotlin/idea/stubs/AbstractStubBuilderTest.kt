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

package org.jetbrains.kotlin.idea.stubs

import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.stubs.StubElement
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.jetbrains.kotlin.psi.stubs.impl.STUB_TO_STRING_PREFIX
import org.jetbrains.kotlin.test.KotlinTestUtils

import java.io.File

abstract class AbstractStubBuilderTest : LightCodeInsightFixtureTestCase() {
    protected fun doTest(sourcePath: String) {
        val file = myFixture.configureByFile(sourcePath) as KtFile
        val jetStubBuilder = KtFileStubBuilder()
        val lighterTree = jetStubBuilder.buildStubTree(file)
        val stubTree = serializeStubToString(lighterTree)
        val expectedFile = sourcePath.replace(".kt", ".expected")
        KotlinTestUtils.assertEqualsToFile(File(expectedFile), stubTree)
    }

    companion object {
        fun serializeStubToString(stubElement: StubElement<*>): String {
            val treeStr = DebugUtil.stubTreeToString(stubElement).replace(SpecialNames.SAFE_IDENTIFIER_FOR_NO_NAME.asString(), "<no name>")

            // Nodes are stored in form "NodeType:Node" and have too many repeating information for Kotlin stubs
            // Remove all repeating information (See KotlinStubBaseImpl.toString())
            return treeStr
                    .lines().map {
                        if (it.contains(STUB_TO_STRING_PREFIX)) {
                            it.takeWhile { it.isWhitespace() } + it.substringAfter("KotlinStub$")
                        }
                        else {
                            it
                        }
                    }
                    .joinToString(separator = "\n")
                    .replace(", [", "[")
        }
    }
}
