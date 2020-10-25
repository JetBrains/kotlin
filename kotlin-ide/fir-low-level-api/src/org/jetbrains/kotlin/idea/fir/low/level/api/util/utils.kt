/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock


internal inline fun <T> executeOrReturnDefaultValueOnPCE(defaultValue: T, action: () -> T): T =
    try {
        action()
    } catch (e: ProcessCanceledException) {
        defaultValue
    }

internal inline fun <T : Any> executeWithoutPCE(crossinline action: () -> T): T {
    var result: T? = null
    ProgressManager.getInstance().executeNonCancelableSection { result = action() }
    return result!!
}

internal inline fun <T : Any> Lock.lockWithPCECheck(lockingIntervalMs: Long, action: () -> T): T {
    var needToRun = true
    var result: T? = null
    while (needToRun) {
        checkCanceled()
        if (tryLock(lockingIntervalMs, TimeUnit.MILLISECONDS)) {
            try {
                needToRun = false
                result = action()
            } finally {
                unlock()
            }
        }
    }
    return result!!
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun checkCanceled() {
    ProgressManager.checkCanceled()
}

internal val FirElement.isErrorElement
    get() = this is FirDiagnosticHolder

internal val FirDeclaration.ktDeclaration: KtDeclaration
    get() {
        val psi = psi
            ?: error("PSI element was not found for${render()}")
        return psi as KtDeclaration
    }


internal fun IdeaModuleInfo.collectTransitiveDependenciesWithSelf(): List<IdeaModuleInfo> {
    val result = mutableSetOf<IdeaModuleInfo>()
    fun collect(module: IdeaModuleInfo) {
        if (module in result) return
        result += module
        module.dependencies().forEach(::collect)
    }
    collect(this)
    return result.toList()
}