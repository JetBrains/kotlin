/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubs

import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.jetbrains.kotlin.psi.stubs.impl.STUB_TO_STRING_PREFIX
import org.jetbrains.kotlin.test.KotlinTestUtils

import java.io.File

abstract class AbstractStubBuilderTest : KotlinLightCodeInsightFixtureTestCase() {
    protected fun doTest(unused: String) {
        val file = myFixture.configureByFile(fileName()) as KtFile
        val ktStubBuilder = KtFileStubBuilder()
        val lighterTree = ktStubBuilder.buildStubTree(file)
        val stubTree = serializeStubToString(lighterTree)
        val expectedFile = testPath().replace(".kt", ".expected")
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
