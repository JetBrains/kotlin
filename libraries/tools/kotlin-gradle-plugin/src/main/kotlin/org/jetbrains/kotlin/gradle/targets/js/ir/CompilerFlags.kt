/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

/**
 * @see [compiler/testData/cli/js/jsExtraHelp.out]
 */

internal const val ENTRY_IR_MODULE = "-Xinclude"

internal const val DISABLE_PRE_IR = "-Xir-only"
internal const val ENABLE_DCE = "-Xir-dce"

internal const val GENERATE_D_TS = "-Xgenerate-dts"

internal const val PRODUCE_JS = "-Xir-produce-js"
internal const val PRODUCE_UNZIPPED_KLIB = "-Xir-produce-klib-dir"