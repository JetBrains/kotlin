/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

sealed interface AnyClassifier

sealed interface AnyClass : CirHasVisibility

sealed interface AnyTypeAlias {
    val underlyingType: AnyType
}

sealed interface AnyType {
    val isMarkedNullable: Boolean
}

sealed interface AnyClassOrTypeAliasType {
    val classifierId: CirEntityId
}