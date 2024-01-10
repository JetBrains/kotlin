/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlinx.jso.compiler.fir

import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlinx.jso.compiler.resolve.JsObjectAnnotations

object JsObjectPredicates {
    internal object AnnotatedWithJsSimpleObject {
        private val jsSimpleObjectAnnotation = setOf(JsObjectAnnotations.jsSimpleObjectAnnotationFqName)
        internal val LOOKUP = LookupPredicate.create { annotated(jsSimpleObjectAnnotation) }
        internal val DECLARATION = DeclarationPredicate.create { annotated(jsSimpleObjectAnnotation) }
    }
}
