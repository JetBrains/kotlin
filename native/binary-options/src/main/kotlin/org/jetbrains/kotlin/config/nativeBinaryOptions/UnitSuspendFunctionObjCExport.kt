/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

enum class UnitSuspendFunctionObjCExport {
    /**
     * In this mode suspend functions and methods with [Unit] return type are exported to Objective-C with an additional argument,
     * continuation callback, with the following Objective-C signature: `(^)(KtKotlinUnit * _Nullable, NSError * _Nullable)`.
     */
    LEGACY,

    /**
     * In this mode suspend functions and methods with [Unit] return type are exported to Objective-C with an additional argument,
     * continuation callback, with the following Objective-C signature: `(^)(NSError * _Nullable)`.
     *
     * Methods overriding superclass methods or implementing interface requirements narrowing down a more general return type
     * to [Unit] are exported like in [LEGACY] mode.
     *
     * Note that in Swift 5.5 and higher suspend functions exported this way are transparently mapped to
     * async functions with `Void` return type
     */
    PROPER,
    ;

    companion object {
        val DEFAULT = PROPER
    }
}