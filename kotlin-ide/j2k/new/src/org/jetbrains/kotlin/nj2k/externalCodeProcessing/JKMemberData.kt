/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.externalCodeProcessing

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

interface JKMemberData<K : KtDeclaration> {
    var kotlinElementPointer: SmartPsiElementPointer<K>?
    var isStatic: Boolean
    val fqName: FqName?
    var name: String

    val kotlinElement
        get() = kotlinElementPointer?.element

    val searchInJavaFiles: Boolean
        get() = true
    val searchInKotlinFiles: Boolean
        get() = true

    val searchingNeeded
        get() = kotlinElement?.isPrivate() != true && (searchInJavaFiles || searchInKotlinFiles)
}

interface JKMemberDataCameFromJava<J : PsiMember, K : KtDeclaration> : JKMemberData<K> {
    val javaElement: J

    override val fqName
        get() = javaElement.getKotlinFqName()
}


interface JKFieldData : JKMemberData<KtProperty>

data class JKFakeFieldData(
    override var isStatic: Boolean,
    override var kotlinElementPointer: SmartPsiElementPointer<KtProperty>? = null,
    override val fqName: FqName?,
    override var name: String
) : JKFieldData {
    override val searchInJavaFiles: Boolean
        get() = false
    override val searchInKotlinFiles: Boolean
        get() = false
}

data class JKFieldDataFromJava(
    override val javaElement: PsiField,
    override var isStatic: Boolean = false,
    override var kotlinElementPointer: SmartPsiElementPointer<KtProperty>? = null,
    override var name: String = javaElement.name
) : JKMemberDataCameFromJava<PsiField, KtProperty>, JKFieldData {
    override val searchInKotlinFiles: Boolean
        get() = wasRenamed

    val wasRenamed: Boolean
        get() = javaElement.name != name
}

data class JKMethodData(
    override val javaElement: PsiMethod,
    override var isStatic: Boolean = false,
    override var kotlinElementPointer: SmartPsiElementPointer<KtNamedFunction>? = null,
    var usedAsAccessorOfProperty: JKFieldData? = null
) : JKMemberDataCameFromJava<PsiMethod, KtNamedFunction> {
    override var name: String = javaElement.name
}