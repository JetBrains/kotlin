/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.stubs

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.jvm.extensions.JvmIrDeclarationOrigin
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

class KaptIrOrigin(backendOrigin: JvmDeclarationOrigin) {
    val declaration: IrDeclaration = (backendOrigin as JvmIrDeclarationOrigin).declaration
    val descriptor: DeclarationDescriptor? = backendOrigin.descriptor
    val element: PsiElement? = backendOrigin.element

    override fun toString(): String = declaration.render()
}
