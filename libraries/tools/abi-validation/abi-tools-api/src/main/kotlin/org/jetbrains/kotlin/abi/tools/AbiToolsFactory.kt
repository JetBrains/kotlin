/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools

/**
 * The abstract factory for obtaining an instance of [AbiTools] - the main class for using the capabilities of ABI Validation tool.
 *
 * @since 2.3.20
 */
public interface AbiToolsFactory {
    /**
     * Gets an instance of [AbiTools].
     *
     * Can be idempotent and return the same instance on each call, but this is not guaranteed.
     */
    public fun get(): AbiTools
}