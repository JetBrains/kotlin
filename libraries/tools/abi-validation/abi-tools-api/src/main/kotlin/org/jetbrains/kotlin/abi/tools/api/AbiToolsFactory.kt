/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.api

/**
 * An abstract factory for obtaining an instance of [AbiToolsInterface] - main class for using the capabilities of ABI Validation tool.
 *
 * @since 2.1.20
 */
public interface AbiToolsFactory {
    /**
     * Gets an instance of [AbiToolsInterface].
     *
     * Can be idempotent and return same instance on each call but this is not guaranteed.
     */
    public fun get(): AbiToolsInterface
}