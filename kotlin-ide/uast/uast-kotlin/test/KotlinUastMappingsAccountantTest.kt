// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.uast.test.kotlin

import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.test.common.PsiClassToString
import org.jetbrains.uast.test.common.UastMappingsAccountantTest
import org.jetbrains.uast.test.common.UastMappingsAccountantTestBase
import org.jetbrains.uast.test.common.sourcesFromDirRecursive
import org.junit.Ignore
import org.junit.Test
import java.io.File


/**
 * Computes Kotlin PSI to UAST mappings over the [TEST_KOTLIN_MODEL_DIR]
 */
@Ignore("Not a test suite, should be called manually only")
class KotlinUastMappingsAccountantTest :
    KotlinLightCodeInsightFixtureTestCase(),
    UastMappingsAccountantTestBase {

    private val ktFileMatcher = ExtensionFileNameMatcher(KotlinFileType.INSTANCE.defaultExtension)

    override fun getTestDataDirectory(): File = TEST_KOTLIN_MODEL_DIR

    override fun getProjectDescriptor(): LightProjectDescriptor =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    private val delegate by lazy(LazyThreadSafetyMode.NONE) {
        UastMappingsAccountantTest(
            sources = sourcesFromDirRecursive(TEST_KOTLIN_MODEL_DIR, ktFileMatcher, myFixture).asIterable(),
            storeResultsTo = TEST_KOTLIN_MODEL_DIR.absoluteFile.parentFile.toPath(),
            resultsNamePrefix = "kotlin-mappings",
            psiClassPrinter = PsiClassToString.kotlinAsIs,
            logger = LOG
        )
    }

    @Test
    override fun `test compute mappings by PSI element and print as trees oriented as child to parent`() =
        delegate.`test compute mappings by PSI element and print as trees oriented as child to parent`()

    @Test
    override fun `test compute mappings by PSI element and print as trees oriented as parent to child`() =
        delegate.`test compute mappings by PSI element and print as trees oriented as parent to child`()

    @Test
    override fun `test compute mappings by PSI element and print as lists oriented as child to parent`() =
        delegate.`test compute mappings by PSI element and print as lists oriented as child to parent`()

    @Test
    override fun `test compute mappings by PSI element and print as lists oriented as parent to child`() =
        delegate.`test compute mappings by PSI element and print as lists oriented as parent to child`()

    @Test
    override fun `test compute mappings by UAST element and print as trees oriented as child to parent`() =
        delegate.`test compute mappings by UAST element and print as trees oriented as child to parent`()

    @Test
    override fun `test compute mappings by UAST element and print as trees oriented as parent to child`() =
        delegate.`test compute mappings by UAST element and print as trees oriented as parent to child`()

    @Test
    override fun `test compute mappings by UAST element and print as lists oriented as child to parent`() =
        delegate.`test compute mappings by UAST element and print as lists oriented as child to parent`()

    @Test
    override fun `test compute mappings by UAST element and print as lists oriented as parent to child`() =
        delegate.`test compute mappings by UAST element and print as lists oriented as parent to child`()

    @Test
    override fun `test compute mappings by UAST element and print as a priory lists of PSI elements`() =
        delegate.`test compute mappings by UAST element and print as a priory lists of PSI elements`()
}