/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.testUtils

import org.jetbrains.kotlin.backend.konan.testUtils.TodoAnalysisApi
import org.junit.AssumptionViolatedException
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler
import kotlin.jvm.optionals.getOrNull

private val kifLocal = System.getProperty("kif.local")?.toBoolean() ?: throw RuntimeException("Missing 'kif.local' System property")

internal class TodoAnalysisApiTestExecutionExceptionHandler : TestExecutionExceptionHandler, AfterEachCallback {
    override fun handleTestExecutionException(context: ExtensionContext, throwable: Throwable) {
        val element = context.element.getOrNull() ?: return
        if (element.isAnnotationPresent(TodoAnalysisApi::class.java)) {
            val message = "Test is marked as 'Todo' for Analysis Api"
            if (!kifLocal) {
                throwable.printStackTrace(System.err)
                throw AssumptionViolatedException(message, throwable)
            } else {
                context.publishReportEntry(message)
                System.err.println(message)
            }
        }

        throw throwable
    }

    override fun afterEach(context: ExtensionContext) {
        val element = context.element.getOrNull() ?: return
        if (element.isAnnotationPresent(TodoAnalysisApi::class.java) && context.executionException.getOrNull() == null) {
            val report: (String) -> Unit = if (!kifLocal) ::error else System.err::println
            report("Test: ${context.displayName} was marked as 'Todo' but executed successfully")
        }
    }
}
