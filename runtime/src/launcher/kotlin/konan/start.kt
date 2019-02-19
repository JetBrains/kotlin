/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.TypedIntrinsic
import kotlin.native.internal.IntrinsicType

// This function is produced by the code generator given
// the '-entry foo.bar.main' flag. 
// It calls the requested entry point.
// The default is main(Array<String>):Unit in the root package.
@TypedIntrinsic(IntrinsicType.SELECT_ENTRY_POINT)
private external fun EntryPointSelector(args: Array<String>)

@SymbolName("OnUnhandledException")
private external fun OnUnhandledException(throwable: Throwable)

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
