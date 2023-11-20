/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.callables

import org.jetbrains.kotlin.formver.viper.ast.Method

/**
 * A callable for which we have a lot of information.
 *
 * This interface does not quite fit along any line that we usually draw, hence the unspecific name. This interface is really two things
 * rolled into one:
 * - A callable with full information about the signature.
 * - A callable which knows how to generate a method header for itself (if it needs one).
 *
 * These are not fundamentally related, but providing two interfaces would mean having to deal with diamond-shaped inheritance
 * in a number of classes that would be quite inconvenient. We thus force users to implement both at once.
 */
interface RichCallableEmbedding : CallableEmbedding, FullNamedFunctionSignature {
    /**
     * Produces a method header (i.e. method without body) corresponding to this function if necessary.
     */
    fun toViperMethodHeader(): Method?
}