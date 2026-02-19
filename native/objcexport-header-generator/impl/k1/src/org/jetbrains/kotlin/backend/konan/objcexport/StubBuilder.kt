/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

internal class StubBuilder<S : ObjCExportStub>(private val problemCollector: ObjCExportProblemCollector) {
    private val children = mutableListOf<S>()

    inline fun add(provider: () -> S) {
        try {
            children.add(provider())
        } catch (t: Throwable) {
            problemCollector.reportException(t)
        }
    }

    operator fun plusAssign(set: Collection<S>) {
        children += set
    }

    fun build(): List<S> = children
}
