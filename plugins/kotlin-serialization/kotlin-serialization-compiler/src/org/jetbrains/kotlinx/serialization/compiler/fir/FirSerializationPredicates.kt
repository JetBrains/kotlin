/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir

import org.jetbrains.kotlin.fir.extensions.predicate.AnnotatedWith
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.ancestorAnnotated
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations

object FirSerializationPredicates {
    internal val serializerFor: DeclarationPredicate =
        AnnotatedWith(setOf(SerializationAnnotations.serializerAnnotationFqName)) // @Serializer(for=...)
    internal val generatedSerializer: DeclarationPredicate =
        ancestorAnnotated(SerializationAnnotations.serializableAnnotationFqName) // @Serializable X.$serializer
    internal val annotatedWithSerializable = AnnotatedWith(setOf(SerializationAnnotations.serializableAnnotationFqName))
}