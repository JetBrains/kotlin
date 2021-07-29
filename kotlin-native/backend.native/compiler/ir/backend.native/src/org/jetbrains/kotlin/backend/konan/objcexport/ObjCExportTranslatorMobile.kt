/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

class ObjCExportTranslatorMobile internal constructor(private val delegate: ObjCExportTranslatorImpl) : ObjCExportTranslator by delegate {
    companion object {
        fun create(namer: ObjCExportNamer): ObjCExportTranslatorMobile {
            val mapper = ObjCExportMapper(local = true)
            return ObjCExportTranslatorMobile(ObjCExportTranslatorImpl(null, mapper, namer, ObjCExportProblemCollector.SILENT, false))
        }
    }

    fun translateBaseFunction(descriptor: FunctionDescriptor): ObjCMethod {
        val classDescriptor = descriptor.containingDeclaration as? ClassDescriptor
        val scope = classDescriptor?.let { delegate.createGenericExportScope(it) } ?: ObjCNoneExportScope
        return delegate.buildMethod(descriptor, descriptor, scope)
    }
}