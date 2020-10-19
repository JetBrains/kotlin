package org.jetbrains.kotlin.idea.structuralsearch

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase
import com.intellij.structuralsearch.MatchOptions
import com.intellij.structuralsearch.impl.matcher.compiler.PatternCompiler
import org.jetbrains.kotlin.idea.KotlinFileType

class KotlinOptimizedSearchScanTest : LightQuickFixTestCase() {

    private fun getSearchPlan(query: String): String {
        val matchOptions = MatchOptions()
        matchOptions.fillSearchCriteria(query)
        matchOptions.fileType = KotlinFileType.INSTANCE
        PatternCompiler.compilePattern(project, matchOptions, true, true)
        return PatternCompiler.getLastSearchPlan()
    }

    fun doTest(message: String, query: String, plan: String) {
        assertEquals(message, plan, getSearchPlan(query))
    }

    fun doTest(query: String, plan: String) {
        assertEquals(plan, getSearchPlan(query))
    }

    fun testClass() {
        doTest("class Foo", "[in code:Foo]")
    }

    fun testNamedFunction() {
        doTest("fun foo(): '_", "[in code:foo]")
    }

    fun testParameter() {
        doTest("fun '_(foo: '_)", "[in code:foo]")
    }

    fun testProperty() {
        doTest("val foo = 1", "[in code:foo]")
    }

    fun testConstantExpression() {
        doTest("val '_ = true", "[in code:true]")
        doTest("val '_ : Int? = null", "[in code:null]")
    }

}