/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities

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
        visibility != DescriptorVisibilities.PRIVATE
                && modality != Modality.FINAL
                && !(containingClassDetails?.modality == Modality.FINAL && containingClassDetails?.kind != ClassKind.ENUM_CLASS)
}
