/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.native.interop.gen.jvm.GenerationMode
import org.jetbrains.kotlin.native.interop.gen.jvm.InteropConfiguration
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.*
import java.io.File
import java.util.*

class StubIrContext(
        val log: (String) -> Unit,
        val configuration: InteropConfiguration,
        val nativeIndex: NativeIndex,
        val imports: Imports,
        val platform: KotlinPlatform,
        val generationMode: GenerationMode,
        val libName: String
) {
    val libraryForCStubs = configuration.library.copy(
            includes = mutableListOf<String>().apply {
                add("stdint.h")
                add("string.h")
                if (platform == KotlinPlatform.JVM) {
                    add("jni.h")
                }
                if (configuration.library.language == Language.CPP) {
                    add("new")
                }
                addAll(configuration.library.includes)
            },
            compilerArgs = configuration.library.compilerArgs,
            additionalPreambleLines = configuration.library.additionalPreambleLines +
                    when (configuration.library.language) {
                        Language.C, Language.CPP -> emptyList()
                        Language.OBJECTIVE_C -> listOf("void objc_terminate();")
                    }
    ).precompileHeaders()

    // TODO: Used only for JVM.
    val jvmFileClassName = if (configuration.pkgName.isEmpty()) {
        libName
    } else {
        configuration.pkgName.substringAfterLast('.')
    }

    val validPackageName = configuration.pkgName.split(".").joinToString(".") {
        if (it.matches(VALID_PACKAGE_NAME_REGEX)) it else "`$it`"
    }

    private val anonymousStructKotlinNames = mutableMapOf<StructDecl, String>()

    private val forbiddenStructNames = run {
        val typedefNames = nativeIndex.typedefs.map { it.name }
        typedefNames.toSet()
    }

    /**
     * The name to be used for this struct in Kotlin
     */
    fun getKotlinName(decl: StructDecl): String {
        val spelling = decl.spelling
        if (decl.isAnonymous) {
            val names = anonymousStructKotlinNames
            return names.getOrPut(decl) {
                "anonymousStruct${names.size + 1}"
            }
        }

        val strippedCName = if (spelling.startsWith("struct ") || spelling.startsWith("union ")) {
            spelling.substringAfter(' ')
        } else {
            spelling
        }

        // TODO: don't mangle struct names because it wouldn't work if the struct
        //   is imported into another interop library.
        return if (strippedCName !in forbiddenStructNames) strippedCName else (strippedCName + "Struct")
    }

    fun addManifestProperties(properties: Properties) {
        val exportForwardDeclarations = configuration.exportForwardDeclarations.toMutableList()

        nativeIndex.structs
                .filter { it.def == null }
                .mapTo(exportForwardDeclarations) {
                    "$cnamesStructsPackageName.${getKotlinName(it)}"
                }

        properties["exportForwardDeclarations"] = exportForwardDeclarations.joinToString(" ")

        // TODO: consider exporting Objective-C class and protocol forward refs.
    }

    companion object {
        private val VALID_PACKAGE_NAME_REGEX = "[a-zA-Z0-9_.]+".toRegex()
    }
}

class StubIrDriver(
        private val context: StubIrContext,
        private val options: DriverOptions
) {
    data class DriverOptions(
            val entryPoint: String?,
            val moduleName: String,
            val outCFile: File,
            val outKtFileCreator: () -> File
    )

    sealed class Result {
        object SourceCode : Result()

        class Metadata(val metadata: KlibModuleMetadata): Result()
    }

    fun run(): Result {
        val (entryPoint, moduleName, outCFile, outKtFile) = options

        val builderResult = StubIrBuilder(context).build()
        val bridgeBuilderResult = StubIrBridgeBuilder(context, builderResult).build()

        outCFile.bufferedWriter().use {
            emitCFile(context, it, entryPoint, bridgeBuilderResult.nativeBridges)
        }

        return when (context.generationMode) {
            GenerationMode.SOURCE_CODE -> {
                emitSourceCode(outKtFile(), builderResult, bridgeBuilderResult)
            }
            GenerationMode.METADATA -> emitMetadata(builderResult, moduleName, bridgeBuilderResult)
        }
    }

    private fun emitSourceCode(
            outKtFile: File, builderResult: StubIrBuilderResult, bridgeBuilderResult: BridgeBuilderResult
    ): Result.SourceCode {
        outKtFile.bufferedWriter().use { ktFile ->
            StubIrTextEmitter(context, builderResult, bridgeBuilderResult).emit(ktFile)
        }
        return Result.SourceCode
    }

    private fun emitMetadata(
            builderResult: StubIrBuilderResult, moduleName: String, bridgeBuilderResult: BridgeBuilderResult
    ) = Result.Metadata(StubIrMetadataEmitter(context, builderResult, moduleName, bridgeBuilderResult).emit())

    private fun emitCFile(context: StubIrContext, cFile: Appendable, entryPoint: String?, nativeBridges: NativeBridges) {
        val out = { it: String -> cFile.appendLine(it) }

        context.libraryForCStubs.preambleLines.forEach {
            out(it)
        }
        out("")

        out("// NOTE THIS FILE IS AUTO-GENERATED")
        out("")

        nativeBridges.nativeLines.forEach { out(it) }

        if (entryPoint != null) {
            out("extern int Konan_main(int argc, char** argv);")
            out("")
            out("__attribute__((__used__))")
            out("int $entryPoint(int argc, char** argv)  {")
            out("  return Konan_main(argc, argv);")
            out("}")
        }
    }
}