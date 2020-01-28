/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

/**
 * @see [compiler/testData/cli/js/jsExtraHelp.out]
 */

const val ENTRY_IR_MODULE = "-Xinclude"

const val DISABLE_PRE_IR = "-Xir-only"
const val ENABLE_DCE = "-Xir-dce"

const val GENERATE_D_TS = "-Xgenerate-dts"

const val PRODUCE_JS = "-Xir-produce-js"
const val PRODUCE_UNZIPPED_KLIB = "-Xir-produce-klib-dir"
const val PRODUCE_ZIPPED_KLIB = "-Xir-produce-klib-file"