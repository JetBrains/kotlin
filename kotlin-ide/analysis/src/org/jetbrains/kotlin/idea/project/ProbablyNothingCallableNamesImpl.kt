/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.idea.caches.trackers.KotlinCodeBlockModificationListener
import org.jetbrains.kotlin.idea.stubindex.KotlinProbablyNothingFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinProbablyNothingPropertyShortNameIndex
import org.jetbrains.kotlin.resolve.lazy.ProbablyNothingCallableNames

class ProbablyNothingCallableNamesImpl(project: Project) : ProbablyNothingCallableNames {
    private val functionNames = createCachedValue(project) { KotlinProbablyNothingFunctionShortNameIndex.getInstance().getAllKeys(project) }
    private val propertyNames = createCachedValue(project) { KotlinProbablyNothingPropertyShortNameIndex.getInstance().getAllKeys(project) }

    override fun functionNames(): Collection<String> = functionNames.value
    override fun propertyNames(): Collection<String> = propertyNames.value
}

private inline fun createCachedValue(project: Project, crossinline names: () -> Collection<String>) =
    CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result.create(names(), KotlinCodeBlockModificationListener.getInstance(project).kotlinOutOfCodeBlockTracker)
        }, false
    )
