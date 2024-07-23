/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.flow.FlowProviders
import org.gradle.api.flow.FlowScope
import org.gradle.build.event.BuildEventsListenerRegistry
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

internal val Project.buildEventsListenerRegistry get() = BuildEventsListenerRegistryHolder.getInstance(this).service
internal val Project.flowProviders get() = FlowProvidersHolder.getInstance(this).service
internal val Project.flowScope get() = FlowScopeHolder.getInstance(this).service

internal open class BuildEventsListenerRegistryHolder @Inject constructor(val service: BuildEventsListenerRegistry) {
    companion object { fun getInstance(project: Project): BuildEventsListenerRegistryHolder = project.objects.newInstance() }
}
internal open class FlowProvidersHolder @Inject constructor(val service: FlowProviders) {
    companion object { fun getInstance(project: Project): FlowProvidersHolder = project.objects.newInstance() }
}
internal open class FlowScopeHolder @Inject constructor(val service: FlowScope) {
    companion object { fun getInstance(project: Project): FlowScopeHolder = project.objects.newInstance() }
}