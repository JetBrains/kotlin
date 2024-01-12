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

private const val VISITORS_PACKAGE = "$BASE_PACKAGE.visitors"

val elementVisitorType = type(VISITORS_PACKAGE, "SirVisitor", TypeKind.Class)
val elementVisitorVoidType = type(VISITORS_PACKAGE, "SirVisitorVoid", TypeKind.Class)
val elementTransformerType = type(VISITORS_PACKAGE, "SirTransformer", TypeKind.Class)
val elementTransformerVoidType = type(VISITORS_PACKAGE, "SirTransformerVoid", TypeKind.Class)

val swiftIrImplementationDetailAnnotation = type(BASE_PACKAGE, "SirImplementationDetail", TypeKind.Class)
val swiftIrBuilderDslAnnotation = type(BASE_PACKAGE, "SirBuilderDsl", TypeKind.Class)
