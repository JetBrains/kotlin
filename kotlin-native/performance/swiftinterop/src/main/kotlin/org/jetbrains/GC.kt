/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

package org.jetbrains.gc

import kotlin.native.runtime.GC

fun collect() = GC.collect()
fun schedule() = GC.schedule()
