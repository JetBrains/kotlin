package com.jetbrains.kotlin.structuralsearch

import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.PatternContext
import com.intellij.structuralsearch.inspection.SSBasedInspection
import com.intellij.structuralsearch.inspection.StructuralSearchProfileActionProvider
import com.intellij.structuralsearch.plugin.ui.SearchConfiguration
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.idea.KotlinFileType

abstract class KotlinSSResourceInspectionTest : BasePlatformTestCase() {
    private var myInspection: SSBasedInspection? = null

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinLightProjectDescriptor()

    override fun setUp() {
        super.setUp()
        myInspection = SSBasedInspection()
        myFixture.enableInspections(myInspection)
    }

    protected fun doTest(pattern: String, context: PatternContext = KotlinStructuralSearchProfile.DEFAULT_CONTEXT) {
        myFixture.configureByFile(getTestName(true) + ".kt")
        val configuration = SearchConfiguration()
        configuration.name = "SSR"
        val options = configuration.matchOptions.apply {
            fileType = KotlinFileType.INSTANCE
            fillSearchCriteria(pattern)
            patternContext = context
        }
        Matcher.validate(project, options)
        StructuralSearchProfileActionProvider.createNewInspection(configuration, project)
        myFixture.testHighlighting(true, false, false)
    }

    override fun getTestDataPath(): String = "src/test/resources/$basePath/"
}