/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt4

import com.intellij.psi.*
import com.sun.tools.javac.tree.JCTree
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.kapt3.base.stubs.KotlinPosition
import org.jetbrains.kotlin.kapt3.base.stubs.getJavacSignature
import org.jetbrains.kotlin.kapt3.stubs.KaptLineMappingCollectorBase

internal class Kapt4LineMappingCollector: KaptLineMappingCollectorBase() {
    fun registerClass(lightClass: PsiClass) {
        register(lightClass, lightClass.qualifiedNameWithSlashes)
    }

    fun registerMethod(lightClass: PsiClass, method: PsiMethod) {
        register(method, lightClass.qualifiedNameWithSlashes + "#" + method.name + method.signature)
    }

    fun registerField(lightClass: PsiClass, field: PsiField) {
        register(field, lightClass.qualifiedNameWithSlashes + "#" + field.name)
    }

    fun registerSignature(declaration: JCTree.JCMethodDecl, method: PsiMethod) {
        signatureInfo[declaration.getJavacSignature()] = method.name + method.signature
    }

    fun getPosition(lightClass: PsiClass): KotlinPosition? {
        return lineInfo[lightClass.qualifiedNameWithSlashes]
    }

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
}
