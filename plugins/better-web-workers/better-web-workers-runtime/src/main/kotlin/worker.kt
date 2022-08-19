/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.webworkers

import org.w3c.dom.Worker
import org.w3c.dom.WorkerGlobalScope

fun <T> worker(block: WorkerGlobalScope.() -> T): Worker = error("intrinsic")