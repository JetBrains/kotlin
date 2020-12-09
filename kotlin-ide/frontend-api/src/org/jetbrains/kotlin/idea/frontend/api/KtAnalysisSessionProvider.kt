/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.utils.PrintingLogger

@RequiresOptIn("To use analysis session, consider using analyze/analyzeWithReadAction/analyseInModalWindow methods")
annotation class InvalidWayOfUsingAnalysisSession

/**
 * Provides [KtAnalysisSession] by [contextElement]
 * Should not be used directly, consider using [analyze]/[analyzeWithReadAction]/[analyseInModalWindow] instead
 */
@InvalidWayOfUsingAnalysisSession
abstract class KtAnalysisSessionProvider {
    @InvalidWayOfUsingAnalysisSession
    abstract fun getAnalysisSessionFor(contextElement: KtElement): KtAnalysisSession
}

@InvalidWayOfUsingAnalysisSession
fun getAnalysisSessionFor(contextElement: KtElement): KtAnalysisSession =
    contextElement.project.service<KtAnalysisSessionProvider>().getAnalysisSessionFor(contextElement)

/**
 * Execute given [action] in [KtAnalysisSession] context
 * Uses [contextElement] to get a module from which you would like to see the other modules
 * Usually [contextElement] is some element form the module you currently analysing now
 *
 * Should not be called from EDT thread
 * Should be called from read action
 * To analyse something from EDT thread, consider using [analyseInModalWindow]
 *
 * @see KtAnalysisSession
 * @see analyzeWithReadAction
 */
@OptIn(InvalidWayOfUsingAnalysisSession::class)
inline fun <R> analyze(contextElement: KtElement, action: KtAnalysisSession.() -> R): R =
    getAnalysisSessionFor(contextElement).action()


/**
 * Execute given [action] in [KtAnalysisSession] context like [analyze] does but execute it in read action
 * Uses [contextElement] to get a module from which you would like to see the other modules
 * Usually [contextElement] is some element form the module you currently analysing now
 *
 * Should be called from read action
 * To analyse something from EDT thread, consider using [analyseInModalWindow]
 * If you are already in read action, consider using [analyze]
 *
 * @see KtAnalysisSession
 * @see analyze
 */
inline fun <R> analyzeWithReadAction(
    contextElement: KtElement,
    crossinline action: KtAnalysisSession.() -> R
): R = runReadAction {
    analyze(contextElement, action)
}

/**
 * Show a modal window with a progress bar and specified [windowTitle]
 * and execute given [action] task with [KtAnalysisSession] context
 * If [action] throws some exception, then [analyseInModalWindow] will rethrow it
 * Should be executed from EDT only
 * If you want to analyse something from non-EDT thread, consider using [analyze]/[analyzeWithReadAction]
 */
inline fun <R> analyseInModalWindow(
    contextElement: KtElement,
    windowTitle: String,
    crossinline action: KtAnalysisSession.() -> R
): R {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val task = object : Task.WithResult<R, Exception>(contextElement.project, windowTitle, /*canBeCancelled*/ true) {
        override fun compute(indicator: ProgressIndicator): R = analyzeWithReadAction(contextElement) { action() }
    }
    task.queue()
    return task.result
}