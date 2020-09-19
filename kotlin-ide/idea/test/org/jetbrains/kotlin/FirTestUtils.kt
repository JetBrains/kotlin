/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

fun <R> executeOnPooledThreadInReadAction(action: () -> R): R? {
    var exception: Exception? = null
    val result = ApplicationManager.getApplication().executeOnPooledThread<R> {
        try {
            runReadAction(action)
        } catch (e: Exception) {
            exception = e
            null
        }
    }.get()

    exception?.run {
        throw this
    }
    return result
}

inline fun doTestWithFIRFlagsByPath(path: String, body: () -> Unit) =
    doTestWithFIRFlags(FileUtil.loadFile(File(path)), body)

inline fun doTestWithFIRFlags(mainFileText: String, body: () -> Unit) {

    if (InTextDirectivesUtils.isDirectiveDefined(mainFileText, "FIR_IGNORE")) return
    val isFirComparison = InTextDirectivesUtils.isDirectiveDefined(mainFileText, "FIR_COMPARISON")

    try {
        body()
    } catch (e: Throwable) {
        if (isFirComparison) throw e
        return
    }
    if (!isFirComparison) {
        throw AssertionError("Looks like test is passing, please add // FIR_COMPARISON at the beginning of the file")
    }
}