/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

object ErrorsNative {
    @JvmField
    val THROWS_LIST_EMPTY = DiagnosticFactory0.create<KtElement>(Severity.ERROR)
    @JvmField
    val INCOMPATIBLE_THROWS_OVERRIDE = DiagnosticFactory1.create<KtElement, DeclarationDescriptor>(Severity.ERROR)
    @JvmField
    val INCOMPATIBLE_THROWS_INHERITED = DiagnosticFactory1.create<KtDeclaration, Collection<DeclarationDescriptor>>(Severity.ERROR)
    @JvmField
    val MUTABLE_ENUM = DiagnosticFactory0.create<KtElement>(Severity.WARNING)
    @JvmField
    val MUTABLE_SINGLETON = DiagnosticFactory0.create<KtElement>(Severity.WARNING)
    @JvmField
    val USELESS_SHARED_IMMUTABLE = DiagnosticFactory0.create<KtElement>(Severity.WARNING)
    @JvmField
    val USELESS_THREAD_LOCAL = DiagnosticFactory0.create<KtElement>(Severity.WARNING)

    init {
        Errors.Initializer.initializeFactoryNames(ErrorsNative::class.java)
    }
}