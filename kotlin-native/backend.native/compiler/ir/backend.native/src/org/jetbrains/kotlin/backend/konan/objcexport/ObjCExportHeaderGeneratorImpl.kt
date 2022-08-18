/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.objcexport.sx.SXClangModuleBuilder
import org.jetbrains.kotlin.backend.konan.reportCompilationWarning
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.source.getPsi

internal class ObjCExportHeaderGeneratorImpl(
        val context: Context,
        moduleDescriptors: List<ModuleDescriptor>,
        mapper: ObjCExportMapper,
        namer: ObjCExportNamer,
        frameworkName: String,
        moduleBuilder: SXClangModuleBuilder,
        eventQueue: EventQueue
) : ObjCExportHeaderGenerator(
        moduleDescriptors,
        mapper,
        namer,
        frameworkName,
        moduleBuilder,
        eventQueue,
) {

    override val shouldExportKDoc = context.shouldExportKDoc()

    override fun getAdditionalImports(): List<String> =
            context.config.configuration.getNotNull(KonanConfigKeys.FRAMEWORK_IMPORT_HEADERS)
}
