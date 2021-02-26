/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.androidnative

import kotlinx.cinterop.*
import platform.android.*

fun main() {
    logInfo("Entering main().")
    memScoped {
        val state = alloc<NativeActivityState>()
        getNativeActivityState(state.ptr)
        val engine = Engine(state)
        try {
            engine.mainLoop()
        } finally {
            engine.dispose()
        }
    }
    kotlin.system.exitProcess(0)
}
