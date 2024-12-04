/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeObjCNameChecker.getObjCNames
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

object NativeObjCNameOverridesChecker : DeclarationChecker {

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is ClassDescriptor) return
        descriptor.defaultType.memberScope
            .getContributedDescriptors(DescriptorKindFilter.ALL, MemberScope.Companion.ALL_NAME_FILTER)
            .forEach {
                if (it !is CallableMemberDescriptor || it.kind.isReal) return@forEach
                check(declaration, it, context)
            }
    }

    fun check(declaration: KtDeclaration, descriptor: CallableMemberDescriptor, context: DeclarationCheckerContext) {
        if (descriptor.overriddenDescriptors.isEmpty()) return
        val objCNames = descriptor.overriddenDescriptors.map { it.getFirstBaseDescriptor().getObjCNames() }
        if (!objCNames.allNamesEquals()) {
            val containingDeclarations = descriptor.overriddenDescriptors.map { it.containingDeclaration }
            context.trace.report(ErrorsNative.INCOMPATIBLE_OBJC_NAME_OVERRIDE.on(declaration, descriptor, containingDeclarations))
        }
    }

    private fun CallableMemberDescriptor.getFirstBaseDescriptor(): CallableMemberDescriptor =
        if (overriddenDescriptors.isEmpty()) this else overriddenDescriptors.first().getFirstBaseDescriptor()

    private fun List<List<NativeObjCNameChecker.ObjCName?>>.allNamesEquals(): Boolean {
        val first = this[0]
        for (i in 1 until size) {
            if (first != this[i]) return false
        }
        return true
    }
}
