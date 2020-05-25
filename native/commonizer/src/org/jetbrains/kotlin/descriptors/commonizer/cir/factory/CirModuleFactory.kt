/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirModule
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirModuleImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.Name

object CirModuleFactory {
    fun create(source: ModuleDescriptor): CirModule = create(source.name.intern())

    fun create(name: Name) = CirModuleImpl(name)
}
