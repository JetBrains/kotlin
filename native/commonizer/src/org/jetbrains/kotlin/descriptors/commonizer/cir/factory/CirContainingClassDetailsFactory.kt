/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirContainingClassDetails
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner

object CirContainingClassDetailsFactory {
    private val interner = Interner<CirContainingClassDetails>()

    // speed optimization
    val DOES_NOT_MATTER: CirContainingClassDetails = create(
        kind = ClassKind.CLASS,
        modality = Modality.FINAL,
        isData = false
    )

    fun create(source: ClassConstructorDescriptor): CirContainingClassDetails = doCreate(source.containingDeclaration)

    fun create(source: CallableMemberDescriptor): CirContainingClassDetails? {
        val containingClass: ClassDescriptor = source.containingDeclaration as? ClassDescriptor ?: return null
        return doCreate(containingClass)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun doCreate(containingClass: ClassDescriptor): CirContainingClassDetails = create(
        kind = containingClass.kind,
        modality = containingClass.modality,
        isData = containingClass.isData
    )

    fun create(
        kind: ClassKind,
        modality: Modality,
        isData: Boolean
    ): CirContainingClassDetails {
        return interner.intern(
            CirContainingClassDetails(
                kind = kind,
                modality = modality,
                isData = isData
            )
        )
    }
}
