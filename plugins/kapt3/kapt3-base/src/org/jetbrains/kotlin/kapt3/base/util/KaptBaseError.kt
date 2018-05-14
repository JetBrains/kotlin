/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base.util

class KaptBaseError : RuntimeException {
    val kind: Kind

    enum class Kind(val message: String) {
        EXCEPTION("Exception while annotation processing"),
        ERROR_RAISED("Error while annotation processing"),
    }

    constructor(kind: Kind) : super(kind.message) {
        this.kind = kind
    }

    constructor(kind: Kind, cause: Throwable) : super(kind.message, cause) {
        this.kind = kind
    }
}