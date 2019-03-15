/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search

import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.junit.Assert

class PsiBasedClassResolverTest : KotlinLightCodeInsightFixtureTestCaseBase() {
    fun testSimple() {
        Assert.assertFalse(doTest("@Test fun foo()")!!)
    }

    fun testImported() {
        Assert.assertTrue(doTest("import org.junit.Test\n\n@Test fun foo()")!!)
    }

    fun testCallReference() {
        Assert.assertNull(doTest("import org.junit.Test\n\nval c = Test()", "Test()"))
    }

    fun testImportStar() {
        Assert.assertTrue(doTest("import org.junit.*\n\n@Test fun foo()")!!)
    }

    fun testImportConflict() {
        Assert.assertFalse(doTest("import org.testng.Test\n\n@Test fun foo()")!!)
    }

    fun testImportConflictOtherName() {
        Assert.assertTrue(doTest("import org.junit.Test\nimport org.testng.NotTest\n\n@Test fun foo()")!!)
    }

    fun testImportStarConflict() {
        Assert.assertFalse(doTest("import org.testng.*\n\n@Test fun foo()")!!)
    }

    fun testImportBoth() {
        Assert.assertNull(doTest("import org.testng.*\nimport org.junit.Test\n@Test fun foo()"))
    }

    fun testImportBoth2() {
        Assert.assertNull(doTest("import org.testng.Test\nimport org.junit.*\n@Test fun foo()"))
    }

    fun testImportAs() {
        Assert.assertNull(doTest("import org.junit.*\nimport java.lang.String as Test\n@Test fun foo()"))
    }

    fun testImportAsSameName() {
        Assert.assertTrue(doTest("import org.junit.Test as Test\n@Test fun foo()")!!)
    }

    fun testThisPackage() {
        Assert.assertTrue(doTest("package org.junit\n\n@Test fun foo()")!!)
    }

    fun testConflictPackageAmbiguity() {
        Assert.assertNull(doTest("package org.testng\n\nimport org.junit.Test\n\n@Test fun foo()"))
    }

    fun testFqName() {
        Assert.assertNull(doTest("val c = org.junit.Test()", "Test"))
    }

    fun testFqNameAnn() {
        Assert.assertNull(doTest("@org.junit.Test fun foo() { }", "@org.junit.Test"))
    }

    fun testOtherFqNameAnn() {
        Assert.assertFalse(doTest("import org.junit.Test\n@foo.bar.Test fun foo() { }", "@foo.bar.Test")!!)
    }

    fun testFqNameWithConflictingImport() {
        Assert.assertNull(doTest("import org.testng.Test\n\nval c = org.junit.Test()", "org.junit.Test"))
    }

    fun testUserType() {
        Assert.assertTrue(doTest("import org.junit.Test\nclass MyTest : Test()", "Test()")!!)
    }

    fun testUserTypeQualified() {
        Assert.assertNull(doTest("class MyTest : org.junit.Test()", "Test()"))
    }

    private fun doTest(fileText: String, marker: String = "@Test"): Boolean? {
        val resolver = PsiBasedClassResolver("org.junit.Test")
        resolver.addConflict("org.testng.Test")
        val file = PsiFileFactory.getInstance(project).createFileFromText("foo.kt", KotlinFileType.INSTANCE,
                                                                          fileText) as KtFile
        val index = fileText.indexOf(marker)
        val ref = file.findElementAt(index + marker.indexOf("Test"))!!.getParentOfType<KtSimpleNameExpression>(false)!!
        val canBeTargetReference = resolver.canBeTargetReference(ref)
        canBeTargetReference.ifSure { return it }
        return null
    }
}
