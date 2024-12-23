/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.config;

import org.jetbrains.kotlin.config.CompilerConfigurationKey;
import org.jetbrains.kotlin.platform.wasm.WasmTarget;

public class WasmConfigurationKeys {
    public static final CompilerConfigurationKey<Boolean> WASM_ENABLE_ARRAY_RANGE_CHECKS =
            CompilerConfigurationKey.create("enable array range checks");

    public static final CompilerConfigurationKey<Boolean> WASM_ENABLE_ASSERTS =
            CompilerConfigurationKey.create("enable asserts");

    public static final CompilerConfigurationKey<Boolean> WASM_GENERATE_WAT =
            CompilerConfigurationKey.create("generate wat file");

    public static final CompilerConfigurationKey<WasmTarget> WASM_TARGET =
            CompilerConfigurationKey.create("wasm target");

    public static final CompilerConfigurationKey<Boolean> WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS =
            CompilerConfigurationKey.create("use wasm traps instead of throwing exceptions");

    public static final CompilerConfigurationKey<Boolean> WASM_USE_NEW_EXCEPTION_PROPOSAL =
            CompilerConfigurationKey.create("use wasm new exception proposal");

    public static final CompilerConfigurationKey<Boolean> WASM_USE_JS_TAG =
            CompilerConfigurationKey.create("use WebAssembly.JSTag to catch JS thrown values");

    public static final CompilerConfigurationKey<Boolean> WASM_DEBUG =
            CompilerConfigurationKey.create("Generate debug information");

    public static final CompilerConfigurationKey<String> DCE_DUMP_DECLARATION_IR_SIZES_TO_FILE =
            CompilerConfigurationKey.create("Path for dumping declaration IR sizes to file");

    public static final CompilerConfigurationKey<Boolean> WASM_GENERATE_DWARF =
            CompilerConfigurationKey.create("generate DWARF debug information");

    public static final CompilerConfigurationKey<Boolean> WASM_FORCE_DEBUG_FRIENDLY_COMPILATION =
            CompilerConfigurationKey.create("avoid optimizations that can break debugging.");
}
