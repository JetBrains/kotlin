/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirAnnotation

/**
 * Commonizes the `kotlin.native.ObjCName` annotation.
 *
 * The annotation is derived from the underlying Objective-C class/protocol name and is therefore expected
 * to be identical across the commonized targets.
 */
object ObjCNameAnnotationCommonizer : AssociativeCommonizer<CirAnnotation> {
    override fun commonize(first: CirAnnotation, second: CirAnnotation): CirAnnotation? {
        // `first`/`second` share the same annotation class id by construction; keep it only if the arguments match.
        return if (first == second) first else null
    }
}
