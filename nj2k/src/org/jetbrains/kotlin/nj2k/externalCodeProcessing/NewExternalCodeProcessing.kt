/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.externalCodeProcessing

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.SmartPointerManager
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.j2k.ExternalCodeProcessing
import org.jetbrains.kotlin.j2k.ProgressPortionReporter
import org.jetbrains.kotlin.j2k.ReferenceSearcher
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.KotlinNJ2KBundle
import org.jetbrains.kotlin.nj2k.fqNameWithoutCompanions
import org.jetbrains.kotlin.nj2k.psi
import org.jetbrains.kotlin.nj2k.tree.JKDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType


class NewExternalCodeProcessing(
    private val referenceSearcher: ReferenceSearcher,
    private val inConversionContext: (PsiElement) -> Boolean
) : ExternalCodeProcessing {
    private val members = mutableMapOf<FqName, JKMemberData<*>>()

    fun addMember(data: JKMemberData<*>) {
        members[data.fqName ?: return] = data
    }

    fun getMember(element: JKDeclaration) =
        element.psi<PsiMember>()?.getKotlinFqName()?.let(members::get)

    fun getMember(fqName: FqName) =
        members[fqName]

    fun isExternalProcessingNeeded(): Boolean =
        members.values.any { it.searchingNeeded }

    private fun List<KtFile>.bindJavaDeclarationsToConvertedKotlinOnes() {
        forEach { file ->
            file.forEachDescendantOfType<KtDeclaration> { declaration ->
                val member = getMember(declaration.fqNameWithoutCompanions) ?: return@forEachDescendantOfType
                when {
                    member is JKFieldData && declaration is KtProperty ->
                        member.kotlinElementPointer = SmartPointerManager.createPointer(declaration)
                    member is JKMethodData && declaration is KtNamedFunction ->
                        member.kotlinElementPointer = SmartPointerManager.createPointer(declaration)
                }
            }
        }
    }

    private fun List<KtFile>.shortenJvmAnnotationsFqNames() {
        val filter = filter@{ element: PsiElement ->
            if (element !is KtUserType) return@filter ShortenReferences.FilterResult.GO_INSIDE
            val isJvmAnnotation = ExternalUsagesFixer.USED_JVM_ANNOTATIONS.any { annotation ->
                element.textMatches(annotation.asString())
            }
            if (isJvmAnnotation) ShortenReferences.FilterResult.PROCESS
            else ShortenReferences.FilterResult.SKIP
        }
        for (file in this) {
            ShortenReferences.DEFAULT.process(file, filter)
        }
    }

    override fun prepareWriteOperation(progress: ProgressIndicator?): (List<KtFile>) -> Unit {
        progress?.text = KotlinNJ2KBundle.message("progress.searching.usages.to.update")

        val usages = mutableListOf<ExternalUsagesFixer.JKMemberInfoWithUsages>()
        for ((index, member) in members.values.withIndex()) {
            if (progress != null) {
                progress.text2 = member.fqName?.shortName()?.identifier ?: continue
                progress.checkCanceled()

                ProgressManager.getInstance().runProcess(
                    { usages += member.collectUsages() },
                    ProgressPortionReporter(progress, index / members.size.toDouble(), 1.0 / members.size)
                )
            } else {
                usages += member.collectUsages()
            }
        }
        return { files ->
            files.bindJavaDeclarationsToConvertedKotlinOnes()
            ExternalUsagesFixer(usages).fix()
            files.shortenJvmAnnotationsFqNames()
        }
    }


    private fun JKMemberData<*>.collectUsages(): ExternalUsagesFixer.JKMemberInfoWithUsages {
        val javaUsages = mutableListOf<PsiElement>()
        val kotlinUsages = mutableListOf<KtElement>()
        if (this is JKMemberDataCameFromJava<*, *>) referenceSearcher.findUsagesForExternalCodeProcessing(
            javaElement,
            searchJava = searchInJavaFiles,
            searchKotlin = searchInKotlinFiles
        ).forEach { usage ->
            val element = usage.element
            if (inConversionContext(element)) return@forEach
            when {
                element is KtElement -> kotlinUsages += element
                element.language == JavaLanguage.INSTANCE -> javaUsages += element
            }
        }
        return ExternalUsagesFixer.JKMemberInfoWithUsages(this, javaUsages, kotlinUsages)
    }
}


