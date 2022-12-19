/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import org.jetbrains.kotlin.backend.konan.OutputFiles
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.getExportedDependencies
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor

/**
 * Create internal representation of Objective-C wrapper.
 */
internal val ProduceObjCExportInterfacePhase = createSimpleNamedCompilerPhase<PhaseContext, FrontendPhaseOutput.Full, ObjCExportedInterface>(
        "ObjCExportInterface",
        "Objective-C header generation",
        outputIfNotEnabled = { _, _, _, _ -> error("Cannot disable `ObjCExportInterface` phase when producing ObjC framework") }
) { context, input ->
    val sharedState = ObjCExportSharedState(listOf(input.moduleDescriptor))
    val translationConfig = sharedState.yieldAll(context.config).toList().single()
    produceObjCExportInterface(
            context,
            translationConfig,
            input.frontendServices,
            sharedState,
    )
}

internal val ProduceMultipleObjCExportInterfacesPhase = createSimpleNamedCompilerPhase<PhaseContext, FrontendPhaseOutput.Full, Map<ModuleTranslationConfig, ObjCExportedInterface>>(
        "ObjCExportInterfaces",
        "Objective-C header generation",
        outputIfNotEnabled = { _, _, _, _ -> emptyMap() }
) { context, input ->
    val sharedState = ObjCExportSharedState(listOf(input.moduleDescriptor) + input.moduleDescriptor.getExportedDependencies(context.config))
    sharedState.yieldAll(context.config).map { moduleTranslationConfig ->
        moduleTranslationConfig to produceObjCExportInterface(context, moduleTranslationConfig, input.frontendServices, sharedState)
    }.onEach { (module, iface) ->
        printExportSummary(module, iface)
    }.toMap()
}

private fun printExportSummary(moduleTranslationConfig: ModuleTranslationConfig, iface: ObjCExportedInterface) {
    val module = moduleTranslationConfig.module
    val isExported = moduleTranslationConfig is ModuleTranslationConfig.Full
    println("Framework ${iface.namer.topLevelNamePrefix} (${module.name}) # ${ if (isExported) "EXPORTED" else "DEPENDENCY" }")
    if (iface.generatedClasses.isNotEmpty()) {
        println("Classes:")
    }
    iface.generatedClasses.forEach { generatedClass ->
        println("- ${generatedClass.name}")
    }
    if (iface.categoryMembers.isNotEmpty()) {
        println("Category members:")
    }
    iface.categoryMembers.forEach { (category, members) ->
        println("- ${category.name}")
        members.forEach { callable ->
            println("- - ${callable.name}")
        }
    }
    if (iface.topLevel.isNotEmpty()) {
        println("Top-level:")
    }
    iface.topLevel.forEach { (file, callables) ->
        println("- ${file.name}")
        callables.forEach { callable ->
            println("- - ${callable.name}")
        }
    }
}

internal data class CreateObjCFrameworkInput(
        val moduleDescriptor: ModuleDescriptor,
        val exportedInterface: ObjCExportedInterface,
        val outputPath: String
)

/**
 * Create Objective-C framework in the given directory without binary.
 */
internal val CreateObjCFrameworkPhase = createSimpleNamedCompilerPhase<PhaseContext, CreateObjCFrameworkInput>(
        "CreateObjCFramework",
        "Create Objective-C framework"
) { context, input ->
    val config = context.config
    // TODO: Share this instance between multiple contexts (including NativeGenerationState)?
    val outputFiles = OutputFiles(input.outputPath, config.target, config.produce)
    println("Creating framework at ${outputFiles.mainFile}")
    createObjCFramework(config, input.moduleDescriptor, input.exportedInterface, outputFiles.mainFile)
}

/**
 * Create specification for bridges between exported Objective-C interfaces and their Kotlin origins.
 */
internal val CreateObjCExportCodeSpecPhase = createSimpleNamedCompilerPhase<PsiToIrContext, ObjCExportedInterface, ObjCExportCodeSpec>(
        "ObjCExportCodeCodeSpec",
        "Objective-C IR symbols",
        outputIfNotEnabled = { _, _, _, _, -> ObjCExportCodeSpec(emptyList(), emptyList()) }
) { context, input ->
    input.createCodeSpec(context.symbolTable!!)
}