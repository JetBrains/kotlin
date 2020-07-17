/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.externalCodeProcessing

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.AccessorKind
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory

internal class ExternalUsagesFixer(private val usages: List<JKMemberInfoWithUsages>) {
    private val conversions = mutableListOf<JKExternalConversion>()

    fun fix() {
        usages.forEach { it.fix() }
        conversions.sort()
        conversions.forEach(JKExternalConversion::apply)
    }

    private fun JKMemberInfoWithUsages.fix() {
        when (member) {
            is JKFieldDataFromJava -> member.fix(javaUsages, kotlinUsages)
            is JKMethodData -> member.fix(javaUsages, kotlinUsages)
        }
    }

    private fun JKFieldDataFromJava.fix(javaUsages: List<PsiElement>, kotlinUsages: List<KtElement>) {
        run {
            val ktProperty = kotlinElement ?: return@run
            when {
                javaUsages.isNotEmpty() && ktProperty.isSimpleProperty() ->
                    ktProperty.addAnnotationIfThereAreNoJvmOnes(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)

                javaUsages.isNotEmpty() && isStatic && !ktProperty.hasModifier(KtTokens.CONST_KEYWORD) ->
                    ktProperty.addAnnotationIfThereAreNoJvmOnes(JVM_STATIC_FQ_NAME)
            }
        }

        if (wasRenamed) {
            javaUsages.forEach { usage ->
                conversions += PropertyRenamedJavaExternalUsageConversion(name, usage)
            }

            kotlinUsages.forEach { usage ->
                conversions += PropertyRenamedKotlinExternalUsageConversion(name, usage)
            }
        }
    }

    private fun JKMethodData.fix(javaUsages: List<PsiElement>, kotlinUsages: List<KtElement>) {
        usedAsAccessorOfProperty?.let { property ->
            val accessorKind =
                if (javaElement.name.startsWith("set")) AccessorKind.SETTER
                else AccessorKind.GETTER

            kotlinUsages.forEach { usage ->
                conversions += AccessorToPropertyKotlinExternalConversion(property.name, accessorKind, usage)
            }
        }
        if (javaUsages.isNotEmpty() && isStatic) {
            when (val accessorOf = usedAsAccessorOfProperty) {
                null -> this
                else -> accessorOf
            }.kotlinElement?.addAnnotationIfThereAreNoJvmOnes(JVM_STATIC_FQ_NAME)
        }
    }

    private fun KtProperty.isSimpleProperty() =
        getter == null
                && setter == null
                && !hasModifier(KtTokens.CONST_KEYWORD)

    private fun KtDeclaration.addAnnotationIfThereAreNoJvmOnes(fqName: FqName) {
        // we don't want to resolve here and as we are working with fqNames, just by-text comparing is OK
        if (annotationEntries.any { entry ->
                USED_JVM_ANNOTATIONS.any { jvmAnnotation ->
                    entry.typeReference?.textMatches(jvmAnnotation.asString()) == true
                }
            }
        ) return
        addAnnotationEntry(KtPsiFactory(this).createAnnotationEntry("@${fqName.asString()}"))
    }

    internal data class JKMemberInfoWithUsages(
        val member: JKMemberData<*>,
        val javaUsages: List<PsiElement>,
        val kotlinUsages: List<KtElement>
    )

    companion object {
        private val JVM_STATIC_FQ_NAME = FqName("kotlin.jvm.JvmStatic")
        val USED_JVM_ANNOTATIONS = listOf(JVM_STATIC_FQ_NAME, JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)
    }
}