/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.native.internal.ExportForCppRuntime

// This function is produced by the code generator given
// the '-entry foo.bar.main' flag. 
// It calls the requested entry point.
// The default is main(Array<String>):Unit in the root package.
@SymbolName("EntryPointSelector")
external fun EntryPointSelector(args: Array<String>)

@SymbolName("OnUnhandledException")
external private fun OnUnhandledException(throwable: Throwable)

@ExportForCppRuntime
private fun Konan_start(args: Array<String>): Int {
    try {
        EntryPointSelector(args)
        // Successfully finished:
        return 0
    } catch (e: Throwable) {
        OnUnhandledException(e)
        return 1
    }
}
