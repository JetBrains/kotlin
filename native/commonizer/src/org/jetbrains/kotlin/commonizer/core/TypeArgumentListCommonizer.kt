/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirTypeProjection
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers

class TypeArgumentListCommonizer(classifiers: CirKnownClassifiers) : AbstractListCommonizer<CirTypeProjection, CirTypeProjection>(
    singleElementCommonizerFactory = { TypeArgumentCommonizer(classifiers) }
)
