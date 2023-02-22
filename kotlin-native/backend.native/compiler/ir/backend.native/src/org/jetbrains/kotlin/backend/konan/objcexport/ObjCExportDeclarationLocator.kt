/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.library.metadata.DeserializedSourceFile
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import org.jetbrains.kotlin.resolve.descriptorUtil.module

/**
 * Find namer for a given declaration across all exported parts.
 */
internal interface ObjCExportDeclarationLocator {
    fun findNamerForDeclaration(descriptor: DeclarationDescriptor): ObjCExportNamer

    fun findNamerForSourceFile(sourceFile: SourceFile): ObjCExportNamer

    companion object {
        fun create(klibMapper: ObjCExportKlibMapper): ObjCExportDeclarationLocator =
                ObjCExportDeclarationLocatorImpl(klibMapper)
    }
}

private class ObjCExportDeclarationLocatorImpl(
        val klibMapper: ObjCExportKlibMapper,
) : ObjCExportDeclarationLocator {
    override fun findNamerForDeclaration(descriptor: DeclarationDescriptor): ObjCExportNamer {
        return klibMapper.getNamerFor(descriptor.module.kotlinLibrary)
    }

    override fun findNamerForSourceFile(sourceFile: SourceFile): ObjCExportNamer {
        when (sourceFile) {
            is DeserializedSourceFile -> {
                return klibMapper.getNamerFor(sourceFile.library)
            }
            else -> error("Can't get namer for $sourceFile")
        }
    }
}