/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.library.KotlinLibrary

/**
 * Defines what LLVM module should consist of.
 */
interface LlvmModuleSpecification {
    val isFinal: Boolean
    fun importsKotlinDeclarationsFromOtherObjectFiles(): Boolean
    fun importsKotlinDeclarationsFromOtherSharedLibraries(): Boolean
    fun containsLibrary(library: KotlinLibrary): Boolean
    fun containsModule(module: ModuleDescriptor): Boolean
    fun containsModule(module: IrModuleFragment): Boolean
    fun containsPackageFragment(packageFragment: IrPackageFragment): Boolean
    fun containsDeclaration(declaration: IrDeclaration): Boolean
}
