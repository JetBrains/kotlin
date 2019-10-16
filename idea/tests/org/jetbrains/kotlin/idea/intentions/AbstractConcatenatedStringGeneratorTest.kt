/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import junit.framework.TestCase
import org.jetbrains.kotlin.idea.intentions.copyConcatenatedStringToClipboard.ConcatenatedStringGenerator
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

/**
 * Compare xxx.kt.result file with the result of ConcatenatedStringGenerator().create(KtBinaryExpression) where KtBinaryExpression is
 * the last KtBinaryExpression of xxx.kt file
 */
abstract class AbstractConcatenatedStringGeneratorTest : KotlinLightCodeInsightFixtureTestCase() {
    @Throws(Exception::class)
    protected fun doTest(path: String) {
        myFixture.configureByFile(fileName())
        val expression = myFixture.file.collectDescendantsOfType<KtBinaryExpression>().lastOrNull()
        TestCase.assertNotNull("No binary expression found: $path", expression)

        val generatedString = ConcatenatedStringGenerator().create(expression!!)

        KotlinTestUtils.assertEqualsToFile(File("${testPath()}.result"), generatedString)
    }
}
