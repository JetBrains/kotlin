/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.nativeBinaryOptions.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.module

interface ObjCExportLazy {
    interface Configuration {
        val frameworkName: String
        val objcGenerics: Boolean
        val objcExportBlockExplicitParameterNames: Boolean
            get() = false

        val disableSwiftMemberNameMangling: Boolean
            get() = false

        val unitSuspendFunctionExport: UnitSuspendFunctionObjCExport
        val ignoreInterfaceMethodCollisions: Boolean
            get() = false

        val entryPoints: ObjCEntryPoints
            get() = ObjCEntryPoints.ALL
    }

    fun generateBase(): List<ObjCTopLevel>
}

@InternalKotlinNativeApi
class ObjCExportLazyImpl(
    languageVersionSettings: LanguageVersionSettings,
    configuration: ObjCExportLazy.Configuration,
    problemCollector: ObjCExportProblemCollector,
    builtIns: KotlinBuiltIns,
) : ObjCExportLazy {

    private val namerConfiguration = createNamerConfiguration(configuration)

    private val mapper = ObjCExportMapper(
        languageVersionSettings,
        local = true,
        configuration.unitSuspendFunctionExport,
        configuration.entryPoints,
    )

    private val namer = ObjCExportNamerImpl(namerConfiguration, builtIns, mapper, problemCollector, local = true)

    private val translator: ObjCExportTranslator = ObjCExportTranslatorImpl(
        null,
        mapper,
        namer,
        problemCollector,
        configuration.objcGenerics,
        configuration.objcExportBlockExplicitParameterNames
    )

    override fun generateBase(): List<ObjCTopLevel> = translator.generateBaseDeclarations()
}

internal fun createNamerConfiguration(configuration: ObjCExportLazy.Configuration): ObjCExportNamer.Configuration {
    return object : ObjCExportNamer.Configuration {
        override val topLevelNamePrefix = abbreviate(configuration.frameworkName)

        override fun getAdditionalPrefix(module: ModuleDescriptor): String? {
            @OptIn(K1Deprecation::class)
            if (module.isNativeStdlib() || module.isCommonStdlibCheckSpecificallyForIDE()) return "Kotlin"
            return null
        }

        override val objcGenerics = configuration.objcGenerics
        override val disableSwiftMemberNameMangling = configuration.disableSwiftMemberNameMangling

        override val ignoreInterfaceMethodCollisions: Boolean = configuration.ignoreInterfaceMethodCollisions
    }
}

private val kotlinSequenceClassId = ClassId.topLevel(FqName("kotlin.sequences.Sequence"))

// This is a special workaround needed for resolve in the IDE.
private fun ModuleDescriptor.isCommonStdlibCheckSpecificallyForIDE() =
    findClassAcrossModuleDependencies(kotlinSequenceClassId)?.module == this

