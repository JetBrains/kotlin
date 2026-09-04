/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

/**
 * Gives access to [BuildEventsListenerRegistry], which is only available via constructor injection
 * and therefore cannot be requested directly from a precompiled script plugin.
 *
 * Instantiate with `objects.newInstance(BuildEventsListenerRegistryHolder::class.java)`.
 */
internal abstract class BuildEventsListenerRegistryHolder @Inject constructor(
    val listenerRegistry: BuildEventsListenerRegistry,
)
