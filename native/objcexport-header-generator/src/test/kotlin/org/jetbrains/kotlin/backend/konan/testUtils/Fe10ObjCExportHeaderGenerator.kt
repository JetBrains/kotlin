/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.testUtils

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.konan.UnitSuspendFunctionObjCExport
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import java.io.File

object Fe10ObjCExportHeaderGenerator : AbstractObjCExportHeaderGeneratorTest.ObjCExportHeaderGenerator {
    override fun generateHeaders(disposable: Disposable, root: File): String {
        val headerGenerator = createObjCExportHeaderGenerator(disposable, root)
        headerGenerator.translateModuleDeclarations()
        return headerGenerator.build().joinToString(System.lineSeparator())
    }

    private fun createObjCExportHeaderGenerator(disposable: Disposable, root: File): ObjCExportHeaderGenerator {
        val mapper = ObjCExportMapper(
            unitSuspendFunctionExport = UnitSuspendFunctionObjCExport.DEFAULT
        )

        val namer = ObjCExportNamerImpl(
            mapper = mapper,
            builtIns = DefaultBuiltIns.Instance,
            local = false,
            problemCollector = ObjCExportProblemCollector.SILENT,
            configuration = object : ObjCExportNamer.Configuration {
                override val topLevelNamePrefix: String get() = ""
                override fun getAdditionalPrefix(module: ModuleDescriptor): String? = null
                override val objcGenerics: Boolean = true
            }
        )

        val environment: KotlinCoreEnvironment = createKotlinCoreEnvironment(disposable)

        val kotlinFiles = root.walkTopDown().filter { it.isFile }.filter { it.extension == "kt" }.toList()

        return ObjCExportHeaderGeneratorImpl(
            moduleDescriptors = listOf(createModuleDescriptor(environment, kotlinFiles)),
            mapper = mapper,
            namer = namer,
            problemCollector = ObjCExportProblemCollector.SILENT,
            objcGenerics = true,
            shouldExportKDoc = true,
            additionalImports = emptyList()
        )
    }
}