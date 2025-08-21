/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.translation

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Definition
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Program
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.TargetLanguage

class CInteropOutput(
    val defFilename: String,
    val defContents: String,
    val headerFilename: String,
    val headerContents: String,
    val args: List<String>,
)

class CInteropConfig(
    val moduleName: String,
)

val CInteropConfig.headerFilename: String
    get() = "$moduleName.h"

fun Program.produceCInterop(config: CInteropConfig): CInteropOutput {
    val context = CInteropTranslationContext(config, GlobalScopeResolver(this))
    context.translate(this)
    return CInteropOutput(
        defFilename = "${config.moduleName}.def",
        defContents = context.defContents.toString(),
        headerFilename = config.headerFilename,
        headerContents = context.headerContents.toString(),
        args = emptyList(),
    )
}

private class CInteropTranslationContext(
    private val config: CInteropConfig,
    private val scopeResolver: GlobalScopeResolver,
    val defContents: OutputFileBuilder = OutputFileBuilder(),
    val headerContents: OutputFileBuilder = OutputFileBuilder(),
) {
    fun translate(program: Program) {
        defContents.raw(
            """
            |headers = ${config.headerFilename}
            |headerFilter = ${config.headerFilename}
            |language = Objective-C
            """.trimMargin()
        )
        headerContents.raw(
            """
            |#include <stdbool.h>
            |#include <stdint.h>
            |#import <Foundation/Foundation.h>
            |
            |@protocol ObjCIndexAccess
            |- (id)loadObjCField:(int32_t)index;
            |- (void)storeObjCField:(int32_t)index value:(id)value;
            |@end
            |
            |bool tryRegisterThread();
            |void unregisterThread();
            |
            |bool updateAllocBlocker();
            |
            |
            """.trimMargin()
        )

        program.definitions.filter { it.targetLanguage is TargetLanguage.ObjC && scopeResolver.isExported(it) }
            .forEach {
                when (it) {
                    is Definition.Function -> translateFunctionDefinition(it)
                    is Definition.Class -> translateClassDefinition(it)
                    is Definition.Global -> error("Exported globals are not supported")
                }
            }
    }

    private fun translateFunctionDefinition(definition: Definition.Function) {
        headerContents.lineEnd {
            scopeResolver.functionObjCDeclaration(this, definition)
            append(";")
        }
    }

    private fun translateClassDefinition(definition: Definition.Class) {
        headerContents.lineEnd("@interface ${scopeResolver.computeName(definition)} : NSObject<ObjCIndexAccess>")
        definition.fields.forEachIndexed { index, _ ->
            headerContents.lineEnd("@property id f${index};")
        }
        if (definition.fields.isNotEmpty()) {
            headerContents.lineEnd {
                scopeResolver.initObjCDeclaration(this, definition)
                append(";")
            }
        }
        headerContents.lineEnd("@end")
        headerContents.lineEnd()
    }
}