/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.library.metadata.kotlinLibrary


internal class ExternalDeclarationFileNameProvider(
        private val moduleDeserializerProvider: ModuleDeserializerProvider
) {
    /**
     * @return a name of the file that contains the [declaration].
     * The key difference from [IrFile.path] is that it supports external package fragments.
     */
    fun getExternalDeclarationFileName(declaration: IrDeclaration): String = when (val packageFragment = declaration.getPackageFragment()) {
        is IrFile -> packageFragment.path

        is IrExternalPackageFragment -> {
            val moduleDescriptor = packageFragment.moduleDescriptor
            val moduleDeserializer = moduleDeserializerProvider.getDeserializerOrNull(moduleDescriptor.kotlinLibrary)
                    ?: error("No module deserializer for $moduleDescriptor")
            moduleDeserializer.getFileNameOf(declaration)
        }

        else -> error("Unknown package fragment kind ${packageFragment::class.java}")
    }

}