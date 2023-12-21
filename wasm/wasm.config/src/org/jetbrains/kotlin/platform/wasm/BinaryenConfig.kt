/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.wasm

object BinaryenConfig {
    val binaryenArgs = listOf(
        // Proposals
        "--enable-gc",
        "--enable-reference-types",
        "--enable-exception-handling",
        "--enable-bulk-memory",  // For array initialization from data sections

        // Other options
        "--enable-nontrapping-float-to-int",
        // It's turned out that it's not safe
        // "--closed-world",

        // Optimizations:
        // Note the order and repetition of the next options matter.
        //
        // About Binaryen optimizations:
        // GC Optimization Guidebook -- https://github.com/WebAssembly/binaryen/wiki/GC-Optimization-Guidebook
        // Optimizer Cookbook -- https://github.com/WebAssembly/binaryen/wiki/Optimizer-Cookbook
        //
        "--inline-functions-with-loops",
        "--traps-never-happen",
        "--fast-math",
        // without "--type-merging" it produces increases the size
        // "--type-ssa",
        "-O3",
        "-O3",
        "--gufa",
        "-O3",
        // requires --closed-world
        // "--type-merging",
        "-O3",
        "-Oz",
    )
}