/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.ir

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.UnwrappedType

data class ExtensionReceiver(
    val annotations: Annotations,
    val type: UnwrappedType
) {
    companion object {
        fun createNoAnnotations(type: UnwrappedType) = ExtensionReceiver(Annotations.EMPTY, type)

        fun ReceiverParameterDescriptor.toReceiver() = ExtensionReceiver(annotations, type.unwrap())
    }
}
