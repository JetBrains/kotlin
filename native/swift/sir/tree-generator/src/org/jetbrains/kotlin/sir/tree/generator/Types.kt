/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.tree.generator

import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.tree.type

val pureAbstractElementType = type(BASE_PACKAGE, "SwiftIrElementBase", TypeKind.Class)
val swiftVisibilityType = type(BASE_PACKAGE, "SwiftVisibility", TypeKind.Class)
val attributeType = type(BASE_PACKAGE, "Attribute", TypeKind.Class)
val originType = type(BASE_PACKAGE, "Origin", TypeKind.Class)

private const val VISITORS_PACKAGE = "$BASE_PACKAGE.visitors"

val elementVisitorType = type(VISITORS_PACKAGE, "SwiftIrVisitor", TypeKind.Class)
val elementTransformerType = type(VISITORS_PACKAGE, "SwiftIrTransformer", TypeKind.Class)

val swiftIrImplementationDetailAnnotation = type(BASE_PACKAGE, "SwiftIrImplementationDetail", TypeKind.Class)
val swiftIrBuilderDslAnnotation = type(BASE_PACKAGE, "SwiftIrBuilderDsl", TypeKind.Class)
