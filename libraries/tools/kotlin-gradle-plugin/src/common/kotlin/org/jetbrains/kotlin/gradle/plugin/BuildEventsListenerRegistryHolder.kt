/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

open class BuildEventsListenerRegistryHolder @Inject constructor(val listenerRegistry: BuildEventsListenerRegistry) {
    companion object {
        fun getInstance(project: Project) =
            project.objects.newInstance(BuildEventsListenerRegistryHolder::class.java)
    }
}
