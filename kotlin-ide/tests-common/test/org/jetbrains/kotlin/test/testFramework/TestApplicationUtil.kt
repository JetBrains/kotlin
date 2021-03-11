/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.testFramework

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.utils.rethrow
import java.lang.reflect.Field

fun resetApplicationToNull(old: Application?) {
    if (old != null) return
    resetApplicationToNull()
}

fun resetApplicationToNull() {
    try {
        val ourApplicationField: Field = ApplicationManager::class.java.getDeclaredField("ourApplication")
        ourApplicationField.setAccessible(true)
        ourApplicationField.set(null, null)
    } catch (e: Exception) {
        throw rethrow(e)
    }
}