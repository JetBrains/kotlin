/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal val KaClassSymbol.isThrowable: Boolean
    get() {
        val classId = classId ?: return false
        return classId.isThrowable
    }

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal val ClassId.isThrowable: Boolean
    get() {
        return StandardNames.FqNames.throwable == this.asSingleFqName()
    }
