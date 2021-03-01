/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlinx.cinterop

import kotlin.native.internal.ExportForCppRuntime

public class ForeignException internal constructor(val nativeException: Any?): Exception() {
    override val message: String = nativeException?.let {
        kotlin_ObjCExport_ExceptionDetails(nativeException)
    }?: ""

    // Current implementation expects NSException type only, which is ensured by CodeGenerator.
    @SymbolName("Kotlin_ObjCExport_ExceptionDetails")
    private external fun kotlin_ObjCExport_ExceptionDetails(nativeException: Any): String?
}

@ExportForCppRuntime
internal fun CreateForeignException(payload: NativePtr): Throwable
        = ForeignException(interpretObjCPointerOrNull<Any?>(payload))
