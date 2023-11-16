/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt3.stubs.KaptLineMappingCollectorBase
import org.jetbrains.org.objectweb.asm.tree.ClassNode

internal class Kapt4LineMappingCollector : KaptLineMappingCollectorBase() {
    fun registerClass(lightClass: PsiClass) {
        register(lightClass, lightClass.qualifiedNameWithSlashes)
    }

    fun registerMethod(lightClass: PsiClass, method: PsiMethod) {
        register(method, lightClass.qualifiedNameWithSlashes + "#" + method.name + method.signature)
    }

    fun registerField(lightClass: PsiClass, field: PsiField) {
        register(field, lightClass.qualifiedNameWithSlashes + "#" + field.name)
    }

    fun getPosition(clazz: PsiClass): KotlinPosition? =
        lineInfo[clazz.qualifiedNameWithSlashes]

    fun getPosition(lightClass: PsiClass, method: PsiMethod): KotlinPosition? =
        lineInfo[lightClass.qualifiedNameWithSlashes + "#" + method.name + method.signature]

    fun getPosition(lightClass: PsiClass, field: PsiField): KotlinPosition? {
        return lineInfo[lightClass.qualifiedNameWithSlashes + "#" + field.name]
    }

    private fun register(asmNode: Any, fqName: String) {
        val psiElement = (asmNode as? KtLightElement<*, *>)?.kotlinOrigin ?: return
        register(fqName, psiElement)
    }

    private val PsiClass.qualifiedNameWithSlashes: String
        get() = qualifiedNameWithDollars?.replace(".", "/") ?: "<no name provided>"

    fun registerSignature(javacSignature: String, method: PsiMethod) {
        signatureInfo[javacSignature] = method.name + method.signature
    }
}
