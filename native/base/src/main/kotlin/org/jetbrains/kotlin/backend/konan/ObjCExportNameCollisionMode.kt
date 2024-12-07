/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

enum class ObjCExportNameCollisionMode {
    /*
     * In this mode, exported ObjC symbol name collisions will not produce any compiler output. 
     */
    NONE,

    /*
     * In this mode, exported ObjC symbol name collisions will produce a compiler warning. 
     */
    WARNING,

    /*
     * In this mode, exported ObjC symbol name collisions will produce a compiler error.
     */
    ERROR,
    ;
}
