/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.classMembers.DependencyMemberInfoModel
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.classMembers.MemberInfoModel
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.utils.ifEmpty

class KotlinInterfaceDependencyMemberInfoModel<T : KtNamedDeclaration, M : MemberInfoBase<T>>(
    aClass: KtClassOrObject
) : DependencyMemberInfoModel<T, M>(KotlinInterfaceMemberDependencyGraph<T, M>(aClass), MemberInfoModel.WARNING) {
    init {
        setTooltipProvider { memberInfo ->
            val dependencies = myMemberDependencyGraph.getDependenciesOf(memberInfo.member).ifEmpty { return@setTooltipProvider null }
            RefactoringBundle.message(
                "interface.member.dependency.required.by.interfaces.list",
                dependencies.size,
                dependencies.joinToString { it.name ?: "" }
            )
        }
    }

    override fun isCheckedWhenDisabled(member: M) = false

    override fun isFixedAbstract(member: M) = null
}