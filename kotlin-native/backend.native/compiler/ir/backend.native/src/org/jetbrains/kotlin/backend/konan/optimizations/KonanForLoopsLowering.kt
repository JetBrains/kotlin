/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.loops.BCEForLoopBodyTransformer
import org.jetbrains.kotlin.backend.common.lower.loops.ForLoopsLowering

class NativeForLoopsLowering(context: CommonBackendContext) : ForLoopsLowering(context) {
    override val loopBodyTransformer = BCEForLoopBodyTransformer()
}
