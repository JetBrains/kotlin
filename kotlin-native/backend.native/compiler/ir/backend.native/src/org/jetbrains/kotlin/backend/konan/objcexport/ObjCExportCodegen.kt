/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.isFinalBinary
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportBlockCodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objcexport.ObjCExportCodeGenerator
import org.jetbrains.kotlin.backend.konan.phases.BitcodegenContext
import org.jetbrains.kotlin.backend.konan.shouldDefineFunctionClasses
import org.jetbrains.kotlin.konan.exec.Command

internal class ObjCExportCodegen(
        private val context: BitcodegenContext,
        private val exportedInterface: ObjCExportedInterface,
        private val codeSpec: ObjCExportCodeSpec,
        private val config: KonanConfig,
) {
    private val target get() = config.target

    lateinit var namer: ObjCExportNamer

    fun generate(codegen: CodeGenerator) {
        if (!target.family.isAppleFamily) return

        if (context.shouldDefineFunctionClasses) {
            ObjCExportBlockCodeGenerator(codegen).generate()
        }

        if (!config.isFinalBinary) return // TODO: emit RTTI to the same modules as classes belong to.

        val mapper = exportedInterface.mapper
        namer = exportedInterface.namer

        val objCCodeGenerator = ObjCExportCodeGenerator(codegen, namer, mapper)

        exportedInterface.generateWorkaroundForSwiftSR10177()

        objCCodeGenerator.generate(codeSpec)
        objCCodeGenerator.dispose()
    }

    // See https://bugs.swift.org/browse/SR-10177
    private fun ObjCExportedInterface.generateWorkaroundForSwiftSR10177() {
        // Code for all protocols from the header should get into the binary.
        // Objective-C protocols ABI is complicated (consider e.g. undocumented extended type encoding),
        // so the easiest way to achieve this (quickly) is to compile a stub by clang.

        val protocolsStub = listOf(
                "__attribute__((used)) static void __workaroundSwiftSR10177() {",
                buildString {
                    append("    ")
                    generatedClasses.forEach {
                        if (it.isInterface) {
                            val protocolName = namer.getClassOrProtocolName(it).objCName
                            append("@protocol($protocolName); ")
                        }
                    }
                },
                "}"
        )

        val source = org.jetbrains.kotlin.konan.file.createTempFile("protocols", ".m").deleteOnExit()
        source.writeLines(headerLines + protocolsStub)

        val bitcode = org.jetbrains.kotlin.konan.file.createTempFile("protocols", ".bc").deleteOnExit()

        val clangCommand = config.clang.clangC(
                source.absolutePath,
                "-O2",
                "-emit-llvm",
                "-c", "-o", bitcode.absolutePath
        )

        val result = Command(clangCommand).getResult(withErrors = true)

        if (result.exitCode == 0) {
            context.llvm.additionalProducedBitcodeFiles += bitcode.absolutePath
        } else {
            // Note: ignoring compile errors intentionally.
            // In this case resulting framework will likely be unusable due to compile errors when importing it.
        }
    }
}
