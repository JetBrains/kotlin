/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.platform

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.PlatformConfiguratorBase
import org.jetbrains.kotlin.resolve.calls.checkers.TypeOfChecker
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.inline.ReasonableInlineRule
import org.jetbrains.kotlin.resolve.jvm.checkers.SuperCallWithDefaultArgumentsChecker
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeSharedImmutableChecker
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeThreadLocalChecker
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeThrowsChecker
import org.jetbrains.kotlin.resolve.konan.diagnostics.NativeTopLevelSingletonChecker

object NativePlatformConfigurator : PlatformConfiguratorBase(
    additionalCallCheckers = listOf(
        SuperCallWithDefaultArgumentsChecker(),
    ),
    additionalDeclarationCheckers = listOf(
        NativeThrowsChecker, NativeSharedImmutableChecker,
        NativeTopLevelSingletonChecker, NativeThreadLocalChecker
    )
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(NativeInliningRule)
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        super.configureModuleDependentCheckers(container)
        container.useImpl<ExpectedActualDeclarationChecker>()
    }
}

object NativeInliningRule : ReasonableInlineRule {
    override fun isInlineReasonable(
        descriptor: CallableMemberDescriptor,
        declaration: KtCallableDeclaration,
        context: BindingContext
    ): Boolean = true
}
