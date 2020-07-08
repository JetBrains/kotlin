/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirDeclaration
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirLiftedUpDeclaration

interface CirNodeWithLiftingUp<T : CirDeclaration, R : CirDeclaration> : CirNode<T, R> {
    val isLiftedUp: Boolean
        get() = (commonDeclaration() as? CirLiftedUpDeclaration)?.isLiftedUp == true
}
