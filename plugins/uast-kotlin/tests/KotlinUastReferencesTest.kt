/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.Disposer
import com.intellij.patterns.uast.injectionHostUExpression
import com.intellij.psi.*
import com.intellij.psi.impl.source.resolve.reference.PsiReferenceContributorEP
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistryImpl
import com.intellij.psi.util.PropertyUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.registerServiceInstance
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.toUElementOfType
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.fail

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class KotlinUastReferencesTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    @Test
    fun `test original getter is visible when reference is under renaming`() {
        KotlinTestUtils.runTest(this) {
            registerReferenceProviders(testRootDisposable) {
                registerUastReferenceProvider(injectionHostUExpression(), uastInjectionHostReferenceProvider { _, psiLanguageInjectionHost ->
                    arrayOf(GetterReference("KotlinBean", psiLanguageInjectionHost))
                })
            }

            myFixture.configureByText(
                "KotlinBean.kt", """
                data class KotlinBean(val myF<caret>ield: String)

                val reference = "myField"

                """.trimIndent()
            )

            myFixture.renameElementAtCaret("myRenamedField")

            myFixture.checkResult(
                """
                data class KotlinBean(val myRenamedField: String)

                val reference = "myRenamedField"

                """.trimIndent()
            )
        }
    }

}

private class GetterReference(
    val className: String,
    psiElement: PsiElement
) : PsiReferenceBase<PsiElement>(psiElement) {
    override fun resolve(): PsiMethod? {
        val psiClass = JavaPsiFacade.getInstance(element.project).findClass(className, element.resolveScope) ?: return null
        val name = element.toUElementOfType<UExpression>()?.evaluateString() ?: return null
        return PropertyUtil.getGetters(psiClass, name).firstOrNull()
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val resolve = resolve()
            ?: fail("can't resolve during rename, looks like someone renamed or removed the source element before updating references")

        val newName =
            if (PropertyUtil.getPropertyName(resolve) != null)
                PropertyUtil.getPropertyName(newElementName) ?: newElementName
            else newElementName

        return super.handleElementRename(newName)
    }

    override fun getVariants(): Array<Any> = emptyArray()
}


fun registerReferenceProviders(disposable: Disposable, registerContributors: PsiReferenceRegistrar.() -> Unit) {
    registerReferenceContributor(object : PsiReferenceContributor() {
        override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) = registrar.registerContributors()
    }, disposable)
}

fun registerReferenceContributor(contributor: PsiReferenceContributor, disposable: Disposable) {
    error("PsiReferenceContributorEP is final in 193 platform")
//    val referenceContributorEp = Extensions.getArea(null).getExtensionPoint<PsiReferenceContributorEP>(PsiReferenceContributor.EP_NAME.name)
//
//    val contributorEp = object : PsiReferenceContributorEP() {
//        override fun getInstance(): PsiReferenceContributor = contributor
//    }
//
//    referenceContributorEp.registerExtension(contributorEp)
//
//    val application = ApplicationManager.getApplication()
//
//    //we need a fresh ReferenceProvidersRegistry after updating ReferenceContributors
//    val oldReferenceProviderRegistry =
//        application.picoContainer.getComponentInstance(ReferenceProvidersRegistry::class.java) as ReferenceProvidersRegistry
//    application.registerServiceInstance(ReferenceProvidersRegistry::class.java, ReferenceProvidersRegistryImpl())
//
//    Disposer.register(disposable, Disposable {
//        referenceContributorEp.unregisterExtension(contributorEp)
//        application.registerServiceInstance(ReferenceProvidersRegistry::class.java, oldReferenceProviderRegistry)
//    })

}




