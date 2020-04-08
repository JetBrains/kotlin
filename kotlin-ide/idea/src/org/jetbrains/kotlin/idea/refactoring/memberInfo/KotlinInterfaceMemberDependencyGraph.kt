/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.psi.PsiMember
import com.intellij.refactoring.classMembers.MemberDependencyGraph
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.util.classMembers.InterfaceMemberDependencyGraph
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.util.*

class KotlinInterfaceMemberDependencyGraph<T : KtNamedDeclaration, M : MemberInfoBase<T>>(
    klass: KtClassOrObject
) : MemberDependencyGraph<T, M> {
    private val delegateGraph = InterfaceMemberDependencyGraph<PsiMember, MemberInfoBase<PsiMember>>(klass.toLightClass())

    override fun memberChanged(memberInfo: M) {
        delegateGraph.memberChanged(memberInfo.toJavaMemberInfo() ?: return)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getDependent() = delegateGraph.dependent
        .asSequence()
        .mapNotNull { it.unwrapped }
        .filterIsInstanceTo(LinkedHashSet<KtNamedDeclaration>()) as Set<T>

    @Suppress("UNCHECKED_CAST")
    override fun getDependenciesOf(member: T): Set<T> {
        val psiMember = lightElementForMemberInfo(member) ?: return emptySet()
        val psiMemberDependencies = delegateGraph.getDependenciesOf(psiMember) ?: return emptySet()
        return psiMemberDependencies
            .asSequence()
            .mapNotNull { it.unwrapped }
            .filterIsInstanceTo(LinkedHashSet<KtNamedDeclaration>()) as Set<T>
    }
}
