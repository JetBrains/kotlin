package com.jetbrains.kotlin.structuralsearch

import com.intellij.openapi.util.text.StringUtil
import com.intellij.structuralsearch.MatchOptions
import com.intellij.structuralsearch.Matcher
import com.intellij.structuralsearch.PatternContext
import com.intellij.structuralsearch.StructuralSearchUtil
import com.intellij.structuralsearch.impl.matcher.CompiledPattern
import com.intellij.structuralsearch.impl.matcher.compiler.PatternCompiler
import com.intellij.structuralsearch.inspection.SSBasedInspection
import com.intellij.structuralsearch.inspection.StructuralSearchProfileActionProvider
import com.intellij.structuralsearch.plugin.ui.SearchConfiguration
import com.intellij.structuralsearch.plugin.ui.UIUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.SmartList
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
        val message = checkApplicableConstraints(options, PatternCompiler.compilePattern(project, options, true, false))
        assertNull("Constraint applicability error: $message\n", message)
        StructuralSearchProfileActionProvider.createNewInspection(configuration, project)
        myFixture.testHighlighting(true, false, false)
    }

    override fun getTestDataPath(): String = "src/test/resources/$basePath/"

    companion object {
        fun checkApplicableConstraints(options: MatchOptions, compiledPattern: CompiledPattern): String? {
            val profile = StructuralSearchUtil.getProfileByFileType(options.fileType)!!
            for (varName in options.variableConstraintNames) {
                val nodes = compiledPattern.getVariableNodes(varName)
                val constraint = options.getVariableConstraint(varName)
                val usedConstraints: MutableList<String> = SmartList()
                if (!StringUtil.isEmpty(constraint.regExp)) usedConstraints.add(UIUtil.TEXT)
                if (constraint.isWithinHierarchy) usedConstraints.add(UIUtil.TEXT_HIERARCHY)
                if (constraint.minCount == 0) usedConstraints.add(UIUtil.MINIMUM_ZERO)
                if (constraint.maxCount > 1) usedConstraints.add(UIUtil.MAXIMUM_UNLIMITED)
                if (!StringUtil.isEmpty(constraint.nameOfExprType)) usedConstraints.add(UIUtil.TYPE)
                if (!StringUtil.isEmpty(constraint.nameOfFormalArgType)) usedConstraints.add(UIUtil.EXPECTED_TYPE)
                if (!StringUtil.isEmpty(constraint.referenceConstraint)) usedConstraints.add(UIUtil.REFERENCE)
                for (usedConstraint in usedConstraints) {
                    if (!profile.isApplicableConstraint(usedConstraint, nodes, false, constraint.isPartOfSearchResults)) {
                        return "$usedConstraint not applicable for $varName"
                    }
                }
            }
            return null
        }
    }
}