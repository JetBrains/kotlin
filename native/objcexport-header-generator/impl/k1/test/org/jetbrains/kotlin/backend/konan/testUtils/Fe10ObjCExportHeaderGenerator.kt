/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.tooling.core.closure
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.io.File


class Fe10HeaderGeneratorExtension : ParameterResolver, AfterEachCallback {

    companion object {
        val namespace = ExtensionContext.Namespace.create(Fe10HeaderGeneratorExtension::class)
        val disposableKey = "disposable"
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == HeaderGenerator::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val disposable = Disposer.newDisposable()
        extensionContext.getStore(namespace).put(disposableKey, disposable)
        return Fe10HeaderGeneratorImpl(disposable)
    }

    override fun afterEach(context: ExtensionContext) {
        val disposable = context.getStore(namespace).get(disposableKey, Disposable::class.java) ?: return
        Disposer.dispose(disposable)
    }
}

private class Fe10HeaderGeneratorImpl(private val disposable: Disposable) : HeaderGenerator {
    override fun generateHeaders(root: File, configuration: HeaderGenerator.Configuration): ObjCHeader {
        val headerGenerator = createObjCExportHeaderGenerator(disposable, root, configuration)

        if (configuration.generateBaseDeclarationStubs) {
            headerGenerator.translateBaseDeclarations()
        }

        headerGenerator.translateModuleDeclarations()
        return headerGenerator.buildHeader()
    }

    private fun createObjCExportHeaderGenerator(
        disposable: Disposable, root: File, configuration: HeaderGenerator.Configuration,
    ): ObjCExportHeaderGenerator {
        val environment: KotlinCoreEnvironment = createKotlinCoreEnvironment(disposable)

        val kotlinFiles = root.walkTopDown().filter { it.isFile }.filter { it.extension == "kt" }.toList()
        val moduleDescriptors = setOf(createModuleDescriptor(environment, kotlinFiles, configuration.dependencies))

        val mapper = ObjCExportMapper(
            unitSuspendFunctionExport = UnitSuspendFunctionObjCExport.DEFAULT
        )

        val exportedModuleDescriptors = moduleDescriptors + moduleDescriptors
            .closure<ModuleDescriptor> { it.allDependencyModules }
            .filter { it.name.asStringStripSpecialMarkers() in configuration.exportedDependencyModuleNames }

        val namer = ObjCExportNamerImpl(
            moduleDescriptors = exportedModuleDescriptors,
            builtIns = DefaultBuiltIns.Instance,
            mapper = mapper,
            problemCollector = ObjCExportProblemCollector.SILENT,
            topLevelNamePrefix = configuration.frameworkName,
            local = false,
            objcGenerics = true,
        )

        return ObjCExportHeaderGeneratorImpl(
            moduleDescriptors = exportedModuleDescriptors.toList(),
            mapper = mapper,
            namer = namer,
            problemCollector = ObjCExportProblemCollector.SILENT,
            objcGenerics = true,
            shouldExportKDoc = true,
            additionalImports = emptyList()
        )
    }
}