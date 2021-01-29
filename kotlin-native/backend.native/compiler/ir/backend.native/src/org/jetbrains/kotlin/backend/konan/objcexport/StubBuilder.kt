/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

internal class StubBuilder<S : Stub<*>>(private val warningCollector: ObjCExportWarningCollector) {
    private val children = mutableListOf<S>()

    inline fun add(provider: () -> S) {
        try {
            children.add(provider())
        } catch (t: Throwable) {
            warningCollector.reportException(t)
        }
    }

    operator fun plusAssign(set: Collection<S>) {
        children += set
    }

    fun build(): List<S> = children
}
