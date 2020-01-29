/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.util.UniqId

internal object EmptyDescriptorTable : DescriptorTable {
    private const val DEFAULT_UNIQ_ID_INDEX = 0L

    override fun put(descriptor: DeclarationDescriptor, uniqId: UniqId) = error("unsupported")
    override fun get(descriptor: DeclarationDescriptor): Long = DEFAULT_UNIQ_ID_INDEX
}
