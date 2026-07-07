/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.unsafe

/**
 * Represents WebAssembly object acting as the namespace for all WebAssembly-related functionality.
 *
 * This is a Kotlin external declaration for JavaScript's [WebAssembly](https://developer.mozilla.org/en-US/docs/WebAssembly/Reference/JavaScript_interface) object.
 *
 */
@ExperimentalWasmJsInterop
@SinceKotlin("2.4") // Effectively it's 2.4.20
public external object WebAssembly {
    /**
     * Represents the linear memory of WebAssembly module.
     *
     * This is a Kotlin external declaration for JavaScript's [WebAssembly.Memory](https://developer.mozilla.org/en-US/docs/WebAssembly/Reference/JavaScript_interface/Memory) object.
     *
     */
    @ExperimentalWasmJsInterop
    @SinceKotlin("2.4") // Effectively it's 2.4.20
    public interface Memory : JsAny
}

/**
 * The linear memory used by the current WebAssembly module.
 *
 * This property provides access to the module's underlying [WebAssembly.Memory] object.
 */
@ExperimentalWasmJsInterop
@SinceKotlin("2.4") // Effectively it's 2.4.20
public val wasmMemory: WebAssembly.Memory
    get() = kotlin.wasm.internal.wasmMemoryInternal()
