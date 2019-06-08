/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.AnalysisScope
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.Nullability
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.NullabilityAnalysisFacade
import org.jetbrains.kotlin.nj2k.nullabilityAnalysis.prepareTypeElementByMakingAllTypesNullable
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractNullabilityAnalysisTest : KotlinLightCodeInsightFixtureTestCase() {

    fun doTest(path: String) {
        val file = File(path)
        val text = FileUtil.loadFile(file, true)
        val ktFile = myFixture.configureByText("converterTestFile.kt", text) as KtFile
        NullabilityAnalysisFacade(
            conversionContext = null,
            getTypeElementNullability = { Nullability.UNKNOWN },
            prepareTypeElement = ::prepareTypeElementByMakingAllTypesNullable,
            debugPrint = false
        )
            .fixNullability(AnalysisScope(ktFile))
        val expectedFile = File(path.replace(".kt", ".kt.after"))
        KotlinTestUtils.assertEqualsToFile(expectedFile, ktFile.text)
    }

    override fun getProjectDescriptor() =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}