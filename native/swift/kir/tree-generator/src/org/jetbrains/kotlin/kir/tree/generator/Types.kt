/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.tree.generator

import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.tree.toAnnotation
import org.jetbrains.kotlin.generators.tree.type

val pureAbstractElementType = type(BASE_PACKAGE, "KirElementBase", TypeKind.Class)
val parameterType = type(BASE_PACKAGE, "KirParameter", TypeKind.Class)

val kirImplementationDetailAnnotation = type(BASE_PACKAGE, "KirImplementationDetail", TypeKind.Class).toAnnotation()
val kirBuilderDslAnnotation = type(BASE_PACKAGE, "KirBuilderDsl", TypeKind.Class).toAnnotation()
