/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubs

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.test.AstAccessControl
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.junit.Assert
import java.io.File

abstract class AbstractResolveByStubTest : KotlinLightCodeInsightFixtureTestCase() {
    protected fun doTest(testFileName: String) {
        doTest(testFileName, true, true)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    private fun doTest(path: String, checkPrimaryConstructors: Boolean, checkPropertyAccessors: Boolean) {
        if (InTextDirectivesUtils.isDirectiveDefined(File(path).readText(), "NO_CHECK_SOURCE_VS_BINARY")) {
            // If NO_CHECK_SOURCE_VS_BINARY is enabled, source vs binary descriptors differ, which means that we should not run this test:
            // it would compare descriptors resolved from sources (by stubs) with .txt, which describes binary descriptors
            return
        }

        myFixture.configureByFile(path)
        val shouldFail = getTestName(false) == "ClassWithConstVal"
        AstAccessControl.testWithControlledAccessToAst(shouldFail, project, testRootDisposable) {
            performTest(path, checkPrimaryConstructors, checkPropertyAccessors)
        }
    }

    private fun performTest(path: String, checkPrimaryConstructors: Boolean, checkPropertyAccessors: Boolean) {
        val file = file as KtFile
        val module = file.findModuleDescriptor()
        val packageViewDescriptor = module.getPackage(FqName("test"))
        Assert.assertFalse(packageViewDescriptor.isEmpty())

        val fileToCompareTo = File(FileUtil.getNameWithoutExtension(path) + ".txt")

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                packageViewDescriptor,
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT
                        .filterRecursion(RecursiveDescriptorComparator.SKIP_BUILT_INS_PACKAGES)
                        .checkPrimaryConstructors(checkPrimaryConstructors)
                        .checkPropertyAccessors(checkPropertyAccessors)
                        .withValidationStrategy(errorTypesForbidden()),
                fileToCompareTo
        )
    }
}
