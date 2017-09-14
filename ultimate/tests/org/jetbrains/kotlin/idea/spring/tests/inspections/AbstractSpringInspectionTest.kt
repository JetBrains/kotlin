package org.jetbrains.kotlin.idea.spring.tests.inspections

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.codeInsight.AbstractInspectionTest
import org.jetbrains.kotlin.idea.spring.tests.configureSpringFileSetByDirective

abstract class AbstractSpringInspectionTest : AbstractInspectionTest() {

    override val forceUsePackageFolder: Boolean = true

    override fun configExtra(psiFiles: List<PsiFile>, options: String) {
        configureSpringFileSetByDirective(myFixture.module, options, psiFiles)
    }
}