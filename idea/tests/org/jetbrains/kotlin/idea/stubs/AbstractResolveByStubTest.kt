/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.junit.Assert
import java.io.File

abstract class AbstractResolveByStubTest : KotlinLightCodeInsightFixtureTestCase() {
    @Throws(Exception::class)
    protected fun doTest(testFileName: String) {
        doTest(testFileName, true, true)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    @Throws(Exception::class)
    private fun doTest(path: String, checkPrimaryConstructors: Boolean, checkPropertyAccessors: Boolean) {
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
