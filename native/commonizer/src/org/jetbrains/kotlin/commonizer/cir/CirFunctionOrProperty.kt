/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities

interface CirFunctionOrProperty :
    CirDeclaration,
    CirHasAnnotations,
    CirHasName,
    CirHasTypeParameters,
    CirHasVisibility,
    CirHasModality,
    CirMaybeCallableMemberOfClass {

    val extensionReceiver: CirExtensionReceiver?
    val returnType: CirType
    val kind: CallableMemberDescriptor.Kind

    fun isVirtual(): Boolean =
        visibility != Visibilities.Private
                && modality != Modality.FINAL
                && !(containingClass?.modality == Modality.FINAL && containingClass?.kind != ClassKind.ENUM_CLASS)
}
