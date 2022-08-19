/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

internal class ObjCExporModulesIndexerImpl(
        val context: Context,
        moduleDescriptors: List<ModuleDescriptor>,
        mapper: ObjCExportMapper,
        eventQueue: EventQueue
) : ObjCExportModulesIndexer(
        moduleDescriptors,
        mapper,
        eventQueue,
) {

    override val shouldExportKDoc = context.shouldExportKDoc()

    override fun getAdditionalImports(): List<String> =
            context.config.configuration.getNotNull(KonanConfigKeys.FRAMEWORK_IMPORT_HEADERS)
}
