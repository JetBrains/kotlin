/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

enum class ObjCExportSuspendFunctionLaunchThreadRestriction {
    /**
     * In this mode, suspend functions called from ObjC/Swift may only be called from the main thread
     */
    MAIN,

    /**
     * In this mode, suspend functions called from ObjC/Swift may be called from any thread
     */
    NONE,
    ;
}
