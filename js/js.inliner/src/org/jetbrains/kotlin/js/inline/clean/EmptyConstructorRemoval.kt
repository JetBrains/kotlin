/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline.clean

import org.jetbrains.kotlin.js.backend.ast.JsClass

class EmptyConstructorRemoval(private val klass: JsClass) {
    fun apply(): Boolean {
        if (klass.constructor?.body?.statements?.isEmpty() != true) return false
        klass.constructor = null
        return true
    }
}