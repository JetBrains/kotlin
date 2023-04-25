/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

fun producePositiveInt(): @Positive Int = 1
fun consumePositiveInt(@Suppress("UNUSED_PARAMETER") x: @Positive Int) {}

fun produceBoxedPositiveInt(): Box<@Positive Int> = Box(1)
