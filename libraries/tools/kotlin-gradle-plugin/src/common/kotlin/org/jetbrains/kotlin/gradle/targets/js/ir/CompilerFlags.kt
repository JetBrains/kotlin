/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

/**
 * @see [compiler/testData/cli/js/jsExtraHelp.out]
 */

internal const val ENABLE_DCE = "-Xir-dce"

internal const val GENERATE_D_TS = "-Xgenerate-dts"

internal const val PRODUCE_UNZIPPED_KLIB = "-Xir-produce-klib-dir"

internal const val MINIMIZED_MEMBER_NAMES = "-Xir-minimized-member-names"

internal const val KLIB_MODULE_NAME = "-Xir-module-name"

internal const val PER_FILE = "-Xir-per-file"
internal const val PER_MODULE = "-Xir-per-module"
internal const val PER_MODULE_OUTPUT_NAME = "-Xir-per-module-output-name"

internal const val WASM_BACKEND = "-Xwasm"
internal const val WASM_TARGET = "-Xwasm-target"
