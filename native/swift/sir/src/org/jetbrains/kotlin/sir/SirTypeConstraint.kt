/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

sealed interface SirTypeConstraint {
    val constraint: SirType
    val subjectPath: List<String>

    data class Equality(
        override val constraint: SirType,
        override val subjectPath: List<String> = emptyList(),
    ) : SirTypeConstraint

    data class Conformance(
        override val constraint: SirType,
        override val subjectPath: List<String> = emptyList(),
    ) : SirTypeConstraint
}