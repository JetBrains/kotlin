/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.sir.SirNamedDeclaration
import org.jetbrains.kotlin.sir.bridge.TypeBindingBridgeRequest
import org.jetbrains.sir.lightclasses.BindableBridgedType

internal fun SirNamedDeclaration.constructTypeBindingBridgeRequests(): List<TypeBindingBridgeRequest> =
    if (this is BindableBridgedType) listOf(TypeBindingBridgeRequest(this)) else emptyList()