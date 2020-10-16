/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction.nonBlocking
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable

fun runActivity(project: Project) {
    nonBlocking(Callable { FileTypeIndex.containsFileOfType(KotlinFileType.INSTANCE, GlobalSearchScope.projectScope(project)) })
        .inSmartMode(project)
        .expireWith(project)
        .finishOnUiThread(ModalityState.any()) { hasKotlinFiles ->
            if (!hasKotlinFiles) return@finishOnUiThread

            val daemonCodeAnalyzer = DaemonCodeAnalyzerImpl.getInstanceEx(project) as DaemonCodeAnalyzerImpl
            daemonCodeAnalyzer.serializeCodeInsightPasses(true)
        }
        .submit(AppExecutorUtil.getAppExecutorService())
}
