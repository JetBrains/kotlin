/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.tree.type

val pureAbstractElementType = type(BASE_PACKAGE, "SirElementBase", TypeKind.Class)
val swiftVisibilityType = type(BASE_PACKAGE, "SirVisibility", TypeKind.Class)
val originType = type(BASE_PACKAGE, "SirOrigin", TypeKind.Class)
val parameterType = type(BASE_PACKAGE, "SirParameter", TypeKind.Class)
val typeType = type(BASE_PACKAGE, "SirType", TypeKind.Class)
val enumCaseType = type(BASE_PACKAGE, "SirEnumCase", TypeKind.Class)
val functionBodyType = type(BASE_PACKAGE, "SirFunctionBody", TypeKind.Class)
val importType = type(BASE_PACKAGE, "SirImport", TypeKind.Class)
val modalityKind = type(BASE_PACKAGE, "SirModality", TypeKind.Class)
val attributeType = type(BASE_PACKAGE, "SirAttribute", TypeKind.Class)
val typeConstraintType = type(BASE_PACKAGE, "SirTypeConstraint", TypeKind.Class)

val swiftIrImplementationDetailAnnotation = type(BASE_PACKAGE, "SirImplementationDetail", TypeKind.Class)
val swiftIrBuilderDslAnnotation = type(BASE_PACKAGE, "SirBuilderDsl", TypeKind.Class)
