/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.*

internal interface ObjCExportGlobalConfig {
    val disableSwiftMemberNameMangling: Boolean

    val ignoreInterfaceMethodCollisions: Boolean

    val objcGenerics: Boolean

    val unitSuspendFunctionExport: UnitSuspendFunctionObjCExport

    val frontendServices: FrontendServices

    val stdlibPrefix: String

    companion object {
        fun create(
                config: KonanConfig,
                frontendServices: FrontendServices,
                stdlibPrefix: String,
        ): ObjCExportGlobalConfig = object : ObjCExportGlobalConfig {
            override val disableSwiftMemberNameMangling: Boolean
                get() = config.configuration.getBoolean(BinaryOptions.objcExportDisableSwiftMemberNameMangling)
            override val ignoreInterfaceMethodCollisions: Boolean
                get() = config.configuration.getBoolean(BinaryOptions.objcExportIgnoreInterfaceMethodCollisions)
            override val objcGenerics: Boolean
                get() = config.configuration.getBoolean(KonanConfigKeys.OBJC_GENERICS)
            override val unitSuspendFunctionExport: UnitSuspendFunctionObjCExport
                get() = config.unitSuspendFunctionObjCExport
            override val frontendServices: FrontendServices
                get() = frontendServices
            override val stdlibPrefix: String
                get() = stdlibPrefix
        }
    }
}