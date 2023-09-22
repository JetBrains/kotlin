/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.targets

interface TargetEventsListener {
    fun onTargetCreated(kotlinTarget: KotlinTarget, context: Any? = null)
}

class AllTargetEventListeners(
    vararg initialListeners: TargetEventsListener
) : TargetEventsListener {
    private val listeners: MutableList<TargetEventsListener> = mutableListOf()

    init {
        listeners.addAll(initialListeners)
    }

    fun add(listener: TargetEventsListener) {
        listeners.add(listener)
    }

    override fun onTargetCreated(kotlinTarget: KotlinTarget, context: Any?) =
        listeners.forEach { it.onTargetCreated(kotlinTarget, context) }
}