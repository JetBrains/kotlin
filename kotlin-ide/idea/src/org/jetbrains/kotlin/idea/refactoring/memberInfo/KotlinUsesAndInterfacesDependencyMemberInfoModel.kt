/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.classMembers.ANDCombinedMemberInfoModel
import com.intellij.refactoring.classMembers.DelegatingMemberInfoModel
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.classMembers.MemberInfoModel
import com.intellij.refactoring.util.classMembers.UsesDependencyMemberInfoModel
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration

open class KotlinUsesAndInterfacesDependencyMemberInfoModel<T : KtNamedDeclaration, M : MemberInfoBase<T>>(
    klass: KtClassOrObject,
    superClass: PsiNamedElement?,
    recursive: Boolean,
    interfaceContainmentVerifier: (T) -> Boolean = { false }
) : DelegatingMemberInfoModel<T, M>(
    ANDCombinedMemberInfoModel(
        object : KotlinUsesDependencyMemberInfoModel<T, M>(klass, superClass, recursive) {
            override fun checkForProblems(memberInfo: M): Int {
                val problem = super.checkForProblems(memberInfo)
                if (problem == MemberInfoModel.OK) return MemberInfoModel.OK

                val member = memberInfo.member
                if (interfaceContainmentVerifier(member)) return MemberInfoModel.OK

                return problem
            }
        },
        KotlinInterfaceDependencyMemberInfoModel<T, M>(klass)
    )
) {
    @Suppress("UNCHECKED_CAST")
    fun setSuperClass(superClass: PsiNamedElement) {
        ((delegatingTarget as ANDCombinedMemberInfoModel<T, M>).model1 as UsesDependencyMemberInfoModel<T, PsiNamedElement, M>).setSuperClass(
            superClass
        )
    }
}
