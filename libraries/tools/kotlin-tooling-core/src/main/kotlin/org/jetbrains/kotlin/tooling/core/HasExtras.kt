/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core


/**
 * Represents a Kotlin DSL entity providing some extra immutable metadata for tooling.
 */
interface HasExtras {

    /**
     * Immutable metadata for tooling.
     */
    val extras: Extras
}

/**
 * Represents a Kotlin DSL entity providing some extra mutable metadata for tooling.
 */
interface HasMutableExtras : HasExtras {

    /**
     * Mutable metadata for tooling.
     */
    override val extras: MutableExtras
}
