/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirModuleBasedSession
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor

@OptIn(PrivateSessionConstructor::class)
abstract class FirIdeSession(override val builtinTypes: BuiltinTypes) : FirSession(sessionProvider = null) {
    abstract val project: Project
}

@OptIn(PrivateSessionConstructor::class)
abstract class FirIdeModuleSession(builtinTypes: BuiltinTypes) : FirIdeSession(builtinTypes) {
    abstract override val moduleInfo: ModuleInfo
    abstract val scope: GlobalSearchScope
}
