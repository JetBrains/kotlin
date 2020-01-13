/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.synthetic.codegen

import kotlinx.android.extensions.CacheImplementation
import org.jetbrains.kotlin.ir.declarations.IrClass

class CliAndroidIrExtension(val isExperimental: Boolean, private val globalCacheImpl: CacheImplementation) : AndroidIrExtension() {
    override fun isEnabled(declaration: IrClass) = true
    override fun isExperimental(declaration: IrClass) = isExperimental
    override fun getGlobalCacheImpl(declaration: IrClass) = globalCacheImpl
}
