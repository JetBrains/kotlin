/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.abi

import org.jetbrains.kotlin.gradle.dsl.NamedDomainImmutableCollection

/**
 * Collection of ABI report variants.
 *
 * A report variant is a set of tasks and filters that are used only for its own variant.
 *
 * @since 2.2.0
 */
interface VariantConfigurator<T : Any> : NamedDomainImmutableCollection<T> {
    /**
     * Create and configure new ABI report variant.
     *
     * When creating a variant, appropriate tasks will be created to generate the dump and check it,
     * and it is possible to specify individual filters for the new variant.
     */
    fun register(name: String, configure: T.() -> Unit)

    /**
     * Create new ABI report variant.
     *
     * When creating a variant, appropriate tasks will be created to generate the dump and check it,
     * and it is possible to specify individual filters for the new variant.
     */
    fun register(name: String)
}