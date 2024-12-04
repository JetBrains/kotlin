/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.bridge

import org.jetbrains.kotlin.sir.util.swiftFqName

public object StableBridgeRequestComparator : Comparator<BridgeRequest> {
    override fun compare(
        lhs: BridgeRequest,
        rhs: BridgeRequest,
    ): Int = when (lhs) {
        is FunctionBridgeRequest -> {
            when (rhs) {
                is FunctionBridgeRequest -> lhs.bridgeName.compareTo(rhs.bridgeName)
                is TypeBindingBridgeRequest -> 1
            }
        }
        is TypeBindingBridgeRequest -> {
            when (rhs) {
                is FunctionBridgeRequest -> -1
                is TypeBindingBridgeRequest -> lhs.sirClass.swiftFqName.compareTo(rhs.sirClass.swiftFqName)
            }
        }
    }
}