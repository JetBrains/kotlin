/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportedInterface
import org.jetbrains.kotlin.backend.konan.objcexport.createCodeSpec
import org.jetbrains.kotlin.backend.konan.objcexport.createObjCFramework
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.kotlinLibrary
import java.io.File

internal data class ProduceObjCExportInterfaceInput(
        val globalConfig: ObjCExportGlobalConfig,
        val structure: ObjCExportStructure,
        val stdlib: KotlinLibrary,
)

internal val ProduceObjCExportMultipleInterfacesPhase = createSimpleNamedCompilerPhase<PhaseContext, ProduceObjCExportInterfaceInput, List<ObjCExportedInterface>>(
        "ObjCExportInterface",
        "Objective-C header generation",
        outputIfNotEnabled = { _, _, _, _ -> error("Cannot disable `ObjCExportInterface` phase when producing ObjC framework") }
) { context, input ->
    val globalConfig = input.globalConfig
    val structure = input.structure
    val frameworkIdProvider = ObjCExportFrameworkIdProvider(structure)
    val stdlibKlib = input.stdlib
    val headerIdProvider = ObjCExportHeaderIdProvider(frameworkIdProvider, structure, stdlibKlib)
    val namerProvider = ObjCNamerProvider(globalConfig, structure)
    val mapper = ObjCExportMapper(globalConfig.frontendServices.deprecationResolver, unitSuspendFunctionExport = globalConfig.unitSuspendFunctionExport)
    val namerProxy = ObjCExportNamerProxy(namerProvider, headerIdProvider)
    val headerToGeneratorMapping = mutableMapOf<ObjCExportFrameworkId, ObjCExportClassGenerator>()

    val headerGenerators = structure.reverseTopSortFrameworks(context.config).associateWith { frameworkStructure ->
        val objCExportHeaderGeneratorImpl = ObjCExportHeaderGeneratorImpl(
                context, frameworkStructure.modulesInfo, mapper, namerProxy,
                namerProvider.getStdlibNamer(),
                objcGenerics = globalConfig.objcGenerics)

        frameworkStructure.modulesInfo.forEach { moduleInfo ->
            val frameworkId = frameworkIdProvider.getFrameworkId(moduleInfo.module.kotlinLibrary)
            headerToGeneratorMapping[frameworkId] = object : ObjCExportClassGenerator {
                override fun requireClassOrInterface(descriptor: ClassDescriptor) {
                    objCExportHeaderGeneratorImpl.requireClassOrInterface(descriptor)
                }

                override fun generateExtraClassEarly(descriptor: ClassDescriptor) {
                    objCExportHeaderGeneratorImpl.generateExtraClassEarly(descriptor)
                }

                override fun generateExtraInterfaceEarly(descriptor: ClassDescriptor) {
                    objCExportHeaderGeneratorImpl.generateExtraInterfaceEarly(descriptor)
                }
            }
        }
        objCExportHeaderGeneratorImpl
    }

    val classGeneratorProvider = ObjCClassGeneratorProvider(headerToGeneratorMapping)
    val listOfDependenciesPerGenerator = mutableMapOf<ObjCExportHeaderGenerator, MutableSet<ObjCExportHeaderId>>()
    headerGenerators.forEach { (structure, headerGenerator) ->
        val classGenerator = ObjCExportClassGeneratorProxy(classGeneratorProvider, headerIdProvider, onHeaderRequested = { headerId ->
            if (structure.headerStrategy.containsHeader(headerId)) {
                return@ObjCExportClassGeneratorProxy
            }
            listOfDependenciesPerGenerator.getOrPut(headerGenerator, ::mutableSetOf) += headerId
        })
        headerGenerator.translateModule(classGenerator)
    }
    while (headerGenerators.values.any { it.hasPendingRequests() }) {
        headerGenerators.values.filter { it.hasPendingRequests() }.forEach { it.processRequests() }
    }
    headerGenerators.map { (frameworkStructure, headerGenerator) ->
        val mandatoryDeps = if (frameworkStructure.modulesInfo.any { it.module.isNativeStdlib() }) {
            emptySet()
        } else {
            setOf(headerIdProvider.getStdlibHeaderId())
        }
        val deps = mandatoryDeps + (listOfDependenciesPerGenerator[headerGenerator] ?: emptySet())
        headerGenerator.buildInterface(deps, ObjCExportFrameworkId(frameworkStructure.name))
    }
}

internal data class CreateObjCFrameworkInput(
        val moduleDescriptor: ModuleDescriptor,
        val exportedInterface: ObjCExportedInterface,
        val frameworkDirectory: File,
)

/**
 * Create Objective-C framework in the given directory without binary.
 */
internal val CreateObjCFrameworkPhase = createSimpleNamedCompilerPhase<PhaseContext, CreateObjCFrameworkInput>(
        "CreateObjCFramework",
        "Create Objective-C framework"
) { context, input ->
    val config = context.config
    createObjCFramework(config, input.moduleDescriptor, input.exportedInterface, input.frameworkDirectory)
}

/**
 * Create specification for bridges between exported Objective-C interfaces and their Kotlin origins.
 */
internal val CreateObjCExportCodeSpecPhase = createSimpleNamedCompilerPhase<PsiToIrContext, ObjCExportedInterface, ObjCExportCodeSpec>(
        "ObjCExportCodeCodeSpec",
        "Objective-C IR symbols",
        outputIfNotEnabled = { _, _, _, _, -> ObjCExportCodeSpec(emptyList(), emptyList(), containsStandardLibrary = false) }
) { context, input ->
    input.createCodeSpec(context.symbolTable!!)
}