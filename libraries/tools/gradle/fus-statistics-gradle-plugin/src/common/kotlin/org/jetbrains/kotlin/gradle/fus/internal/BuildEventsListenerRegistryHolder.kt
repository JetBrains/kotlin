/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.internal

import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

internal abstract class BuildEventsListenerRegistryHolder @Inject constructor(
    val listenerRegistry: BuildEventsListenerRegistry
)