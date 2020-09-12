/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor

@OptIn(PrivateSessionConstructor::class)
class FirIdeBuiltinsAndCloneableSession @PrivateSessionConstructor constructor(
    override val project: Project,
    builtinTypes: BuiltinTypes,
) : FirIdeSession(builtinTypes)