/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dispatcher.common

import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FqnUtils {
    object DispatchedVisitor {
        val DISPATCH_FUNCTION_NAME = Name.identifier("dispatch")
        val DISPATCH_FUNCTION_NODE_ARGUMENT_NAME = Name.identifier("expr")

        val DISPATCHED_VISITOR_ANNOTATION_FQN = AnnotationFqn("org.jetbrains.kotlin.dispatcher.DispatchedVisitor")
        val GENERATE_DISPATCH_FUNCTION_FQN = AnnotationFqn("org.jetbrains.kotlin.dispatcher.GenerateDispatchFunction")

        val DISPATCHED_ANNOTATION_FQN = AnnotationFqn("org.jetbrains.kotlin.dispatcher.Dispatched")
    }

    object Kind {
        val GET_KIND_FUNCTION_NAME = Name.identifier("getKind")

        val WITH_ABSTRACT_KIND_ANNOTATION_FQN = AnnotationFqn("org.jetbrains.kotlin.dispatcher.WithAbstractKind")
        val WITH_KIND_ANNOTATION_FQN = AnnotationFqn("org.jetbrains.kotlin.dispatcher.WithKind")
    }
}