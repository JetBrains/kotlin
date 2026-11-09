/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer.internal

import org.jetbrains.dokka.links.DRI
import org.jetbrains.kotlin.documentation.KdElementId

/**
 * A [DRI] (Dokka Resource Identifier) already uniquely and deterministically identifies a declaration independently
 * of which source sets it ends up merged into, which is exactly what [KdElementId] needs: the same declaration gets
 * the same ID in every fragment it's accessible from (see `KdFragment`), other than the `actual typealias` case
 * covered by `KdExpectActualMapping`.
 *
 * TODO KT-90192
 */
internal fun DRI.toKdElementId(): KdElementId = KdElementId(toString())
