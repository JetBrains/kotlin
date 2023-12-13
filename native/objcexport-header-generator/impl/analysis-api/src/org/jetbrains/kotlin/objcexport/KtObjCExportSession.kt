/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

interface KtObjCExportSession {
    val configuration: KtObjCExportConfiguration
}

inline fun <T> KtObjCExportSession(
    configuration: KtObjCExportConfiguration,
    block: KtObjCExportSession.() -> T,
): T {
    return object : KtObjCExportSession {
        override val configuration: KtObjCExportConfiguration = configuration
    }.block()
}