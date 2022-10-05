/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.specialization.common

import org.jetbrains.kotlin.fir.extensions.AnnotationFqn

object FqnUtils {
    val MONOMORPHIC_ANNOTATION_FQN = AnnotationFqn("org.jetbrains.kotlin.specialization.Monomorphic")
}