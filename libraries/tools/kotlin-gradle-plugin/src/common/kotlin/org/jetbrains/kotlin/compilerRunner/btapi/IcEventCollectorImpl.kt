/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.trackers.IcEvent
import org.jetbrains.kotlin.buildtools.api.trackers.IcEventCollector

class IcEventCollectorImpl : IcEventCollector {

    private val events: MutableList<IcEvent> = mutableListOf()
    val collectedEvents: List<IcEvent> get() = events

    override fun collectEvents(eventsFromBta: List<IcEvent>) {
        events.addAll(eventsFromBta)
    }
}
