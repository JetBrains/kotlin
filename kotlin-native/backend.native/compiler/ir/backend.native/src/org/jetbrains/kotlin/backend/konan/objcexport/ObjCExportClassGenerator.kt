/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ClassDescriptor

interface ObjCExportClassGenerator {
    /**
     * Require generation of Objective-C declaration for the [descriptor].
     */
    fun requireClassOrInterface(descriptor: ClassDescriptor)
}


interface ObjCExportClassGeneratorRegistry {
    fun register(frameworkId: ObjCExportFrameworkId, classGenerator: ObjCExportClassGenerator)
}

class ObjCExportClassGeneratorProxy(
        private val classGeneratorProvider: ObjCClassGeneratorProvider,
        private val headerIdProvider: ObjCExportHeaderIdProvider,
        private val onHeaderRequested: (ObjCExportHeaderId) -> Unit = {}
) : ObjCExportClassGenerator {
    override fun requireClassOrInterface(descriptor: ClassDescriptor) {
        val headerId = headerIdProvider.getHeaderId(descriptor)
        onHeaderRequested(headerId)
        classGeneratorProvider.getClassGenerator(headerId).requireClassOrInterface(descriptor)
    }
}