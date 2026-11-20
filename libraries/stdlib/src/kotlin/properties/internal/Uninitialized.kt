/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.properties.internal

/**
 * A sentinel value representing an uninitialized `lateinit val` property state.
 *
 * Refer to [KEEP-0445](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0455-lateinit-val.md#compilation-strategy)
 * for additional information about `lateinit val`s and `UNINITIALIZED` value.
 *
 * ### Implementation details
 * On JVM, the value is serializable.
 *
 * It is a published API as the generated code will depend on it.
 */
@PublishedApi
@SinceKotlin("2.5")
internal expect object UNINITIALIZED
