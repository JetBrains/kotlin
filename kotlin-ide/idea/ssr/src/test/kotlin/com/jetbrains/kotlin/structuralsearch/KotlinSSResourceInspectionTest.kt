package com.jetbrains.kotlin.structuralsearch

import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.inspection.highlightTemplate.SSBasedInspection
import com.intellij.structuralsearch.plugin.ui.SearchConfiguration
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import java.util.*

@Suppress("UnstableApiUsage")
abstract class KotlinSSResourceInspectionTest : BasePlatformTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinLightProjectDescriptor()

    protected fun doTest(pattern: String) {
        myFixture.configureByFile(getTestName(true) + ".kt")
        val configuration = SearchConfiguration()
        configuration.name = "SSR"
        val options = configuration.matchOptions.apply {
            fileType = KotlinFileType.INSTANCE
            fillSearchCriteria(pattern)
        }
        Matcher.validate(project, options)

        val inspection = SSBasedInspection()
        inspection.setConfigurations(Collections.singletonList(configuration))
        myFixture.enableInspections(inspection)

        myFixture.testHighlighting(true, false, false)
    }

    override fun getTestDataPath(): String = "src/test/resources/$basePath/"
}