/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config

enum class DceRuntimeDiagnostic {
    LOG,
    EXCEPTION;

    companion object
}

fun DceRuntimeDiagnostic.removingBody(): Boolean {
    return this != DceRuntimeDiagnostic.LOG
}

fun DceRuntimeDiagnostic.dceRuntimeDiagnosticToArgumentOfUnreachableMethod(): Int {
    return when (this) {
        DceRuntimeDiagnostic.LOG -> 0
        DceRuntimeDiagnostic.EXCEPTION -> 1
    }
}
