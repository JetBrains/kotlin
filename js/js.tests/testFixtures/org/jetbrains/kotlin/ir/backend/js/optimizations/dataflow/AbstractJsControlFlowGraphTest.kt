/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.backend.js.optimizations.JsOptimizationIrTestBase

/**
 * Base for on-disk JS CFG dump tests under `js/js.tests/testData/dataflow`.
 */
abstract class AbstractJsControlFlowGraphTest : JsOptimizationIrTestBase(::JsControlFlowGraphHandler)
