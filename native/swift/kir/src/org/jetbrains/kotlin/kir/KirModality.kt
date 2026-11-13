/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir

enum class KirModality {
    OPEN,
    FINAL,
    SEALED,
    ABSTRACT,
    OVERRIDE,
    UNSPECIFIED,
}

val KirClassMemberDeclaration.effectiveModality: KirModality
    get() = when (val parent = this.parent) {
        is KirClass -> when (parent.modality) {
            KirModality.FINAL -> FINAL
            else -> this.modality
        }
        else -> this.modality
    }
