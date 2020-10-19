package org.jetbrains.kotlin.idea.structuralsearch.search.filters

import org.jetbrains.kotlin.idea.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSCountFilterTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "countFilter"

    // isApplicableMinCount

    fun testMinProperty() { doTest("var '_ = '_{0,0}") }

    fun testMinDotQualifierExpression() { doTest("'_{0,0}.'_") }

    fun testMinFunctionTypeReference() { doTest("fun '_{0,0}.'_()") }

    fun testMinCallableReferenceExpression() { doTest("'_{0,0}::'_") }

    fun testMinWhenExpression() { doTest("when ('_{0,0}) {}") }

    fun testMinConstructorCallee() { doTest("class '_ : '_{0,0}('_*)") }

    fun testMinSuperType() { doTest("class '_ : '_{0,0}()") }

    // isApplicableMaxCount

    fun testMaxDestructuringDeclarationEntry() { doTest("for (('_{3,3}) in '_) { '_* }") }

    fun testMaxWhenConditionWithExpression() { doTest("when ('_?) { '_{2,2} -> '_ }") }

    // isApplicableMinMaxCount

    fun testMmClassBodyElement() {
        doTest(
            """
        class '_Class {  
            var '_Field{0,2} = '_Init?
        }
        """
        )
    }

    fun testMmParameter() { doTest("fun '_('_{0,2})") }

    fun testMmTypeParameter() { doTest("fun <'_{0,2}> '_('_*)") }

    fun testMmTypeParameterFunctionType() { doTest("fun '_('_ : ('_{0,2}) -> '_)") }

    fun testMmTypeReference() { doTest("val '_ : ('_{0,2}) -> '_") }

    fun testMmSuperTypeEntry() { doTest("class '_ : '_{0,2}") }

    fun testMmValueArgument() { doTest("listOf('_{0,2})") }

    fun testMmStatementInDoWhile() { doTest("do { '_{0,2} } while ('_)") }

    fun testMmStatementInBlock() { doTest("fun '_('_*) { '_{0,2} }") }

    fun testMmAnnotation() { doTest("@'_{0,2} class '_") }

    fun testMmSimpleNameStringTemplateEntry() { doTest(""" "$$'_{0,2}" """) }
    
    fun testMmTypeProjection() { doTest("fun '_('_ : '_<'_{0,2}>)") }

    fun testMmKDocTag() { doTest("""
        /**
         * @'_{0,2}
         */
    """.trimIndent()) }
}