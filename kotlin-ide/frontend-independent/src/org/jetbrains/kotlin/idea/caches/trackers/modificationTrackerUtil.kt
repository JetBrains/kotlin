/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.trackers

import com.intellij.openapi.module.Module
import com.intellij.openapi.util.ModificationTracker

fun getLatestModificationCount(modules: Collection<Module>): Long {
    if (modules.isEmpty())
        return ModificationTracker.NEVER_CHANGED.modificationCount

    val modificationCountUpdater =
        KotlinCodeBlockModificationListener.getInstance(modules.first().project).perModuleOutOfCodeBlockTrackerUpdater
    return modules.maxOfOrNull { modificationCountUpdater.getModificationCount(it) }
        ?: ModificationTracker.NEVER_CHANGED.modificationCount
}
