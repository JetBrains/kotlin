/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.transformers

import org.jetbrains.kotlin.tools.projectWizard.core.andThenNullable

typealias TransformerFunction<I> = (I) -> I?
typealias Predicate<T> = (T) -> Boolean

fun <T : Any> Iterable<TransformerFunction<T>>.foldTransformers() =
    fold<TransformerFunction<T>, TransformerFunction<T>>({ null }) { acc, transformer ->
        acc andThenNullable transformer
    }

fun <T> Iterable<Predicate<T>>.foldPredicates() = { value: T ->
    all { it.invoke(value) }
}