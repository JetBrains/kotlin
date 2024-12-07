/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.ModuleDescriptor

internal class ObjCExportHeaderGeneratorImpl(
    moduleDescriptors: List<ModuleDescriptor>,
    mapper: ObjCExportMapper,
    namer: ObjCExportNamer,
    problemCollector: ObjCExportProblemCollector,
    objcGenerics: Boolean,
    override val shouldExportKDoc: Boolean,
    private val additionalImports: List<String>,
) : ObjCExportHeaderGenerator(moduleDescriptors, mapper, namer, objcGenerics, problemCollector) {
    override fun getAdditionalImports(): List<String> =
        additionalImports
}
