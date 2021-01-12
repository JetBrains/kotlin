// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.stats.storage.factors.LookupStorage
import junit.framework.TestCase

class CompletionLoggingPsiFeaturesTest: CompletionLoggingTestBase() {
  fun `test psi parent feature for position after qualifier`() {
    myFixture.configureByText(JavaFileType.INSTANCE, "public class HelloWorld {\n" +
                                                     "    public static void main(String[] args) {\n" +
                                                     "        System.<caret>\n" +
                                                     "    }\n" +
                                                     "}")
    myFixture.completeBasic()
    checkParentsPsiIs("PsiExpressionStatementImpl",
                      "PsiCodeBlockImpl",
                      "PsiMethodImpl",
                      "PsiClassImpl",
                      "PsiJavaFileImpl")
  }

  fun `test psi parent feature in if statement`() {
    myFixture.configureByText(JavaFileType.INSTANCE, "public class HelloWorld {\n" +
                                                     "    public static void main(String[] args) {\n" +
                                                     "        if (<caret>\n" +
                                                     "    }\n" +
                                                     "}")
    myFixture.completeBasic()
    checkParentsPsiIs("PsiIfStatementImpl",
                      "PsiCodeBlockImpl",
                      "PsiMethodImpl",
                      "PsiClassImpl",
                      "PsiJavaFileImpl")
  }

  fun `test psi parent feature in arguments context`() {
    myFixture.configureByText(JavaFileType.INSTANCE, "public class HelloWorld {\n" +
                                                     "    public static void main(String[] args) {\n" +
                                                     "        String a = \"42\";\n" +
                                                     "        int b = 42;\n" +
                                                     "        System.out.println(<caret>\n" +
                                                     "    }\n" +
                                                     "}")
    myFixture.completeBasic()
    checkParentsPsiIs("PsiExpressionListImpl",
                      "PsiMethodCallExpressionImpl",
                      "PsiExpressionStatementImpl",
                      "PsiCodeBlockImpl",
                      "PsiMethodImpl",
                      "PsiClassImpl",
                      "PsiJavaFileImpl")
  }

  fun `test is after dot true`() {
    myFixture.configureByText(JavaFileType.INSTANCE, "public class HelloWorld {\n" +
                                                     "    public static void main(String[] args) {\n" +
                                                     "        System.out.<caret>\n" +
                                                     "    }\n" +
                                                     "}")
    myFixture.completeBasic()
    checkHaveFeatures(mapOf("ml_ctx_common_is_after_dot" to true))
  }

  fun `test is after dot false`() {
    myFixture.configureByText(JavaFileType.INSTANCE, "public class HelloWorld {\n" +
                                                     "    public static void main(String[] args) {\n" +
                                                     "        <caret>\n" +
                                                     "    }\n" +
                                                     "}")
    myFixture.completeBasic()
    checkHaveFeatures(mapOf("ml_ctx_common_is_after_dot" to false))
  }

  private fun checkParentsPsiIs(vararg expectedParents: String) {
    val features = LookupStorage.get(lookup)?.contextProvidersResult()!!
    expectedParents.forEachIndexed() { i, expectedParent ->
      val actualParent = features.classNameValue("ml_ctx_common_parent_${i + 1}")
      TestCase.assertEquals("Psi parent features", expectedParent, actualParent)
    }

    // There are no unchecked parent features
    TestCase.assertNull(features.classNameValue("ml_ctx_common_parent_${expectedParents.size + 1}"))
  }

  private fun checkHaveFeatures(expectedFeatures: Map<String, Any>) {
    val features = LookupStorage.get(lookup)?.contextProvidersResult()!!
    expectedFeatures.forEach { fName, fValue ->
      TestCase.assertTrue(features.binaryValue(fName) == fValue)
    }
  }
}