/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import org.jetbrains.kotlin.config.nativeBinaryOptions.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportMapper
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportProblemCollector
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver

internal fun createObjCExportNamerConfiguration(
    topLevelNamePrefix: String = "",
    additionalPrefix: (moduleName: Name) -> String? = { null },
): ObjCExportNamer.Configuration {
    return object : ObjCExportNamer.Configuration {
        override val topLevelNamePrefix: String get() = topLevelNamePrefix
        override fun getAdditionalPrefix(module: ModuleDescriptor): String? = additionalPrefix(module.name)
        override val objcGenerics: Boolean = true
    }
}

internal fun createObjCExportMapper(
    deprecationResolver: DeprecationResolver? = null,
    local: Boolean = false,
    unitSuspendFunctionObjCExport: UnitSuspendFunctionObjCExport = UnitSuspendFunctionObjCExport.DEFAULT,
): ObjCExportMapper {
    return ObjCExportMapper(deprecationResolver, local, unitSuspendFunctionObjCExport)
}

internal fun createObjCExportNamer(
    configuration: ObjCExportNamer.Configuration = createObjCExportNamerConfiguration(),
    builtIns: KotlinBuiltIns = DefaultBuiltIns.Instance,
    local: Boolean = false,
    problemCollector: ObjCExportProblemCollector = ObjCExportProblemCollector.SILENT,
    mapper: ObjCExportMapper = createObjCExportMapper(local = local),
): ObjCExportNamer {
    return ObjCExportNamerImpl(
        builtIns = builtIns,
        mapper = mapper,
        local = local,
        problemCollector = problemCollector,
        configuration = configuration
    )
}