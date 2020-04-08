/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReference
import com.intellij.refactoring.move.moveMembers.MoveJavaMemberHandler
import com.intellij.refactoring.move.moveMembers.MoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import org.jetbrains.kotlin.idea.references.getImportAlias

class MoveKotlinMemberHandler : MoveJavaMemberHandler() {
    override fun getUsage(
        member: PsiMember,
        psiReference: PsiReference,
        membersToMove: MutableSet<PsiMember>,
        targetClass: PsiClass
    ): MoveMembersProcessor.MoveMembersUsageInfo? {
        psiReference?.getImportAlias()?.let {
            return null
        }
        return super.getUsage(member, psiReference, membersToMove, targetClass)
    }

    override fun changeExternalUsage(options: MoveMembersOptions, usage: MoveMembersProcessor.MoveMembersUsageInfo): Boolean {
        val reference = usage.getReference()
        reference?.getImportAlias()?.let { return true }

        return super.changeExternalUsage(options, usage)
    }
}