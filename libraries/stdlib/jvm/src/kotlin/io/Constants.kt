/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("ConstantsKt")

package kotlin.io

/**
 * Returns the default buffer size when working with buffered streams.
 */
public const val DEFAULT_BUFFER_SIZE: Int = 8 * 1024

/**
 * Returns the default block size for forEachBlock().
 */
internal const val DEFAULT_BLOCK_SIZE: Int = 4096
/**
 * Returns the minimum block size for forEachBlock().
 */
internal const val MINIMUM_BLOCK_SIZE: Int = 512