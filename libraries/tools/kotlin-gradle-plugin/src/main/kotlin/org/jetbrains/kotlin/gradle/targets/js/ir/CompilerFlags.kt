/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.jetbrains.kotlin.gradle.dsl.KotlinJsOptions

/**
 * @see [compiler/testData/cli/js/jsExtraHelp.out]
 */

internal const val ENTRY_IR_MODULE = "-Xinclude"

internal const val DISABLE_PRE_IR = "-Xir-only"
internal const val ENABLE_DCE = "-Xir-dce"

internal const val GENERATE_D_TS = "-Xgenerate-dts"

internal const val PRODUCE_JS = "-Xir-produce-js"
internal const val PRODUCE_UNZIPPED_KLIB = "-Xir-produce-klib-dir"
internal const val PRODUCE_ZIPPED_KLIB = "-Xir-produce-klib-file"

internal const val MINIMIZED_MEMBER_NAMES = "-Xir-minimized-member-names"

internal const val MODULE_NAME = "-Xir-module-name"

internal const val PER_MODULE = "-Xir-per-module"
internal const val PER_MODULE_OUTPUT_NAME = "-Xir-per-module-output-name"

internal const val WASM_BACKEND = "-Xwasm"

fun KotlinJsOptions.isProduceUnzippedKlib() = PRODUCE_UNZIPPED_KLIB in freeCompilerArgs
fun KotlinJsOptions.isProduceZippedKlib() = PRODUCE_ZIPPED_KLIB in freeCompilerArgs