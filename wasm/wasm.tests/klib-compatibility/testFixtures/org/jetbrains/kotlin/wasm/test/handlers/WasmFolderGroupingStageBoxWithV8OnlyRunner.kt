/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.handlers

import org.jetbrains.kotlin.test.services.TestServices

/**
 * A [WasmFolderGroupingStageBoxRunner] that executes the box on V8 only.
 *
 * KLIB-compatibility tests set up only the V8 engine, so the other engines (SpiderMonkey, JavaScriptCore)
 * must not be referenced there. It's enough to execute an image on any single reliable engine, V8.
 */
class WasmFolderGroupingStageBoxWithV8OnlyRunner(
    testServices: TestServices,
) : WasmFolderGroupingStageBoxRunner(testServices) {
    override val executeWithV8Only: Boolean = true
}
