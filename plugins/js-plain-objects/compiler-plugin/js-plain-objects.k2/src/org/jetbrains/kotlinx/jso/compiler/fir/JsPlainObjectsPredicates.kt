/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlinx.jspo.compiler.fir

import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlinx.jspo.compiler.resolve.JsPlainObjectsAnnotations

object JsPlainObjectsPredicates {
    internal object AnnotatedWithJsPlainObject {
        private val jsPlainObjectAnnotation = setOf(JsPlainObjectsAnnotations.jsPlainObjectAnnotationFqName)
        internal val LOOKUP = LookupPredicate.create { annotated(jsPlainObjectAnnotation) }
        internal val DECLARATION = DeclarationPredicate.create { annotated(jsPlainObjectAnnotation) }
    }
}
