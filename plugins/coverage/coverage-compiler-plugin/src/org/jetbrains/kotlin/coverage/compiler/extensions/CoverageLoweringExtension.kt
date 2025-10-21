/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package org.jetbrains.kotlin.coverage.compiler.extensions

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.coverage.compiler.instrumentation.IrFileVisitor
import org.jetbrains.kotlin.coverage.compiler.common.KotlinCoverageInstrumentationContext
import org.jetbrains.kotlin.coverage.compiler.hit.HitRegistrarFactory
import org.jetbrains.kotlin.coverage.compiler.instrumentation.LineBranchInstrumenter
import org.jetbrains.kotlin.coverage.compiler.metadata.ModuleIM
import org.jetbrains.kotlin.coverage.compiler.metadata.writeToFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.platform.jvm.isJvm
import java.io.File
import kotlin.random.Random

class CoverageLoweringExtension(val modulePath: String, val metadataFilePath: String) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        if (!pluginContext.platform.isJvm()) {
            // only JVM platform supported for now
            return
        }

        val context = KotlinCoverageInstrumentationContext(pluginContext)

        val hitRegistrarFactory = HitRegistrarFactory()
        val instrumenter = LineBranchInstrumenter()

        val moduleDir = File(modulePath)

        val moduleId = moduleFragment.name.asString().hashCode() and Random.nextInt()

        val moduleIM = ModuleIM()

        moduleFragment.files.forEach { file ->
            val relativePath = File(file.path).toRelativeString(moduleDir)
            val fileIM = moduleIM.addFile(relativePath, file.packageFqName.asString())

            val segmentGenerator = hitRegistrarFactory.create(moduleId, fileIM.number, context)
            IrFileVisitor(file, fileIM, instrumenter, segmentGenerator, context).process()
        }

        moduleIM.writeToFile(File(metadataFilePath))
    }
}
