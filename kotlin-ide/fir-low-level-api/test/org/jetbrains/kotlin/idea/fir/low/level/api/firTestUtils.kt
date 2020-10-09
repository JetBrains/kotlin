/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProviderStorage
import org.jetbrains.kotlin.psi.KtElement

internal fun Project.allModules() = ModuleManager.getInstance(this).modules.toList()

inline fun resolveWithClearCaches(context: KtElement, action: (FirModuleResolveState) -> Unit) {
    val resolveState = createResolveStateForNoCaching(context.getModuleInfo())
    action(resolveState)
}