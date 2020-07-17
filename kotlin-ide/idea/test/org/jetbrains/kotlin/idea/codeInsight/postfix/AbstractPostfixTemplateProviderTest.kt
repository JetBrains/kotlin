/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.postfix

import org.jetbrains.kotlin.idea.liveTemplates.setTemplateTestingCompat
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractPostfixTemplateProviderTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    override fun setUp() {
        super.setUp()
        KtPostfixTemplateProvider.previouslySuggestedExpressions = emptyList()
    }

    protected fun doTest(unused: String) {
        val testFile = testDataFile()
        myFixture.configureByFile(testFile)

        val fileText = file.text
        val template = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// TEMPLATE:")
        if (template != null) {
            setTemplateTestingCompat(project, testRootDisposable)
            myFixture.type(template.replace("\\t", "\t"))
        } else {
            myFixture.type('\t')
        }

        val previouslySuggestedExpressions = KtPostfixTemplateProvider.previouslySuggestedExpressions
        if (previouslySuggestedExpressions.size > 1 && !InTextDirectivesUtils.isDirectiveDefined(fileText, "ALLOW_MULTIPLE_EXPRESSIONS")) {
            fail("Only one expression should be suggested, but $previouslySuggestedExpressions were found")
        }

        val expectedFile = File(testFile.parentFile, testFile.name + ".after")
        myFixture.checkResultByFile(expectedFile)
    }

    override fun tearDown() {
        super.tearDown()
        KtPostfixTemplateProvider.previouslySuggestedExpressions = emptyList()
    }
}
