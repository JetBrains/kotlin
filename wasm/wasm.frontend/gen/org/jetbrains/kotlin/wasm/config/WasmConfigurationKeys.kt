/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IncorrectFormatting", "unused")

package org.jetbrains.kotlin.wasm.config

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.platform.wasm.WasmTarget

object WasmConfigurationKeys {
    @JvmField
    val WASM_ENABLE_ARRAY_RANGE_CHECKS = CompilerConfigurationKey.create<Boolean>("WASM_ENABLE_ARRAY_RANGE_CHECKS")

    @JvmField
    val WASM_ENABLE_ASSERTS = CompilerConfigurationKey.create<Boolean>("WASM_ENABLE_ASSERTS")

    @JvmField
    val WASM_GENERATE_WAT = CompilerConfigurationKey.create<Boolean>("WASM_GENERATE_WAT")

    @JvmField
    val WASM_TARGET = CompilerConfigurationKey.create<WasmTarget>("WASM_TARGET")

    @JvmField
    val WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS = CompilerConfigurationKey.create<Boolean>("WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS")

    @JvmField
    val WASM_USE_NEW_EXCEPTION_PROPOSAL = CompilerConfigurationKey.create<Boolean>("WASM_USE_NEW_EXCEPTION_PROPOSAL")

    // Don't use WebAssembly.JSTag for throwing and catching exceptions
    @JvmField
    val WASM_NO_JS_TAG = CompilerConfigurationKey.create<Boolean>("WASM_NO_JS_TAG")

    @JvmField
    val WASM_DEBUG = CompilerConfigurationKey.create<Boolean>("WASM_DEBUG")

    @JvmField
    val DCE_DUMP_DECLARATION_IR_SIZES_TO_FILE = CompilerConfigurationKey.create<String>("DCE_DUMP_DECLARATION_IR_SIZES_TO_FILE")

    @JvmField
    val WASM_GENERATE_DWARF = CompilerConfigurationKey.create<Boolean>("WASM_GENERATE_DWARF")

    @JvmField
    val WASM_FORCE_DEBUG_FRIENDLY_COMPILATION = CompilerConfigurationKey.create<Boolean>("WASM_FORCE_DEBUG_FRIENDLY_COMPILATION")

    @JvmField
    val WASM_INCLUDED_MODULE_ONLY = CompilerConfigurationKey.create<Boolean>("WASM_INCLUDED_MODULE_ONLY")

    // Provide alternative paths to imported dependency modules.
    @JvmField
    val WASM_DEPENDENCY_RESOLUTION_MAP = CompilerConfigurationKey.create<String>("WASM_DEPENDENCY_RESOLUTION_MAP")

    // Use command module initialization (_initialize export).
    @JvmField
    val WASM_COMMAND_MODULE = CompilerConfigurationKey.create<Boolean>("WASM_COMMAND_MODULE")

    // Disables cross-file optimizations. Required to for IC.
    @JvmField
    val WASM_DISABLE_CROSS_FILE_OPTIMISATIONS = CompilerConfigurationKey.create<Boolean>("WASM_DISABLE_CROSS_FILE_OPTIMISATIONS")

    // Prefix for the name of internal/synthetic local variables.
    @JvmField
    val WASM_INTERNAL_LOCAL_VARIABLE_PREFIX = CompilerConfigurationKey.create<String>("WASM_INTERNAL_LOCAL_VARIABLE_PREFIX")

}

var CompilerConfiguration.wasmEnableArrayRangeChecks: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS)
    set(value) { put(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, value) }

var CompilerConfiguration.wasmEnableAsserts: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_ENABLE_ASSERTS)
    set(value) { put(WasmConfigurationKeys.WASM_ENABLE_ASSERTS, value) }

var CompilerConfiguration.wasmGenerateWat: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_GENERATE_WAT)
    set(value) { put(WasmConfigurationKeys.WASM_GENERATE_WAT, value) }

var CompilerConfiguration.wasmTarget: WasmTarget
    get() = get(WasmConfigurationKeys.WASM_TARGET, WasmTarget.JS)
    set(value) { put(WasmConfigurationKeys.WASM_TARGET, value) }

var CompilerConfiguration.wasmUseTrapsInsteadOfExceptions: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS)
    set(value) { put(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS, value) }

var CompilerConfiguration.wasmUseNewExceptionProposal: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL)
    set(value) { put(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL, value) }

var CompilerConfiguration.wasmNoJsTag: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_NO_JS_TAG)
    set(value) { put(WasmConfigurationKeys.WASM_NO_JS_TAG, value) }

var CompilerConfiguration.wasmDebug: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_DEBUG)
    set(value) { put(WasmConfigurationKeys.WASM_DEBUG, value) }

var CompilerConfiguration.dceDumpDeclarationIrSizesToFile: String?
    get() = get(WasmConfigurationKeys.DCE_DUMP_DECLARATION_IR_SIZES_TO_FILE)
    set(value) { put(WasmConfigurationKeys.DCE_DUMP_DECLARATION_IR_SIZES_TO_FILE, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.wasmGenerateDwarf: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_GENERATE_DWARF)
    set(value) { put(WasmConfigurationKeys.WASM_GENERATE_DWARF, value) }

var CompilerConfiguration.wasmForceDebugFriendlyCompilation: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_FORCE_DEBUG_FRIENDLY_COMPILATION)
    set(value) { put(WasmConfigurationKeys.WASM_FORCE_DEBUG_FRIENDLY_COMPILATION, value) }

var CompilerConfiguration.wasmIncludedModuleOnly: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_INCLUDED_MODULE_ONLY)
    set(value) { put(WasmConfigurationKeys.WASM_INCLUDED_MODULE_ONLY, value) }

var CompilerConfiguration.wasmDependencyResolutionMap: String?
    get() = get(WasmConfigurationKeys.WASM_DEPENDENCY_RESOLUTION_MAP)
    set(value) { put(WasmConfigurationKeys.WASM_DEPENDENCY_RESOLUTION_MAP, requireNotNull(value) { "nullable values are not allowed" }) }

var CompilerConfiguration.wasmCommandModule: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_COMMAND_MODULE)
    set(value) { put(WasmConfigurationKeys.WASM_COMMAND_MODULE, value) }

var CompilerConfiguration.wasmDisableCrossFileOptimisations: Boolean
    get() = getBoolean(WasmConfigurationKeys.WASM_DISABLE_CROSS_FILE_OPTIMISATIONS)
    set(value) { put(WasmConfigurationKeys.WASM_DISABLE_CROSS_FILE_OPTIMISATIONS, value) }

var CompilerConfiguration.wasmInternalLocalVariablePrefix: String?
    get() = get(WasmConfigurationKeys.WASM_INTERNAL_LOCAL_VARIABLE_PREFIX)
    set(value) { put(WasmConfigurationKeys.WASM_INTERNAL_LOCAL_VARIABLE_PREFIX, requireNotNull(value) { "nullable values are not allowed" }) }

