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
        myFixture.configureByFile(path)
        val expression = myFixture.file.collectDescendantsOfType<KtBinaryExpression>().lastOrNull()
        TestCase.assertNotNull("No binary expression found: $path", expression)

        val generatedString = ConcatenatedStringGenerator().create(expression!!)

        KotlinTestUtils.assertEqualsToFile(File("$path.result"), generatedString)
    }
}
