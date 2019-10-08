/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.inference.mutability

import org.jetbrains.kotlin.nj2k.inference.common.DefaultStateProvider
import org.jetbrains.kotlin.nj2k.inference.common.State
import org.jetbrains.kotlin.nj2k.inference.common.TypeVariable

class MutabilityDefaultStateProvider : DefaultStateProvider() {
    override fun defaultStateFor(typeVariable: TypeVariable): State =
        State.UPPER
}