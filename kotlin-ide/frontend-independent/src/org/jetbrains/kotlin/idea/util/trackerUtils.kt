/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import kotlin.reflect.KProperty

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> CachedValue<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

inline fun <T> cachedValue(project: Project, vararg dependencies: Any, crossinline createValue: () -> T) =
    CachedValuesManager.getManager(project).createCachedValue {
        CachedValueProvider.Result(
            createValue(),
            dependencies
        )
    }

/**
 * Creates a value which will be cached until until any physical PSI change happens
 *
 * @see com.intellij.psi.util.CachedValue
 * @see com.intellij.psi.util.PsiModificationTracker.MODIFICATION_COUNT
 */
fun <T> psiModificationTrackerBasedCachedValue(project: Project, createValue: () -> T) =
    cachedValue(project, PsiModificationTracker.MODIFICATION_COUNT, createValue = createValue)