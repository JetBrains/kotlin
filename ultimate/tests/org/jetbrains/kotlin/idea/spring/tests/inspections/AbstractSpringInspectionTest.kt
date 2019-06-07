package org.jetbrains.kotlin.idea.spring.tests.inspections

import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.codeInsight.AbstractInspectionTest
import org.jetbrains.kotlin.idea.spring.tests.configureSpringFileSetByDirective
import org.jetbrains.kotlin.idea.spring.tests.forbidSpringFileSetAutoConfigureByDirective

abstract class AbstractSpringInspectionTest : AbstractInspectionTest() {

    override val forceUsePackageFolder: Boolean = true

    override fun configExtra(psiFiles: List<PsiFile>, options: String) {
        forbidSpringFileSetAutoConfigureByDirective(project, options)
        configureSpringFileSetByDirective(myFixture.module, options, psiFiles)
    }
}