/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

internal class StubBuilder {
    private val children = mutableListOf<Stub<*>>()

    operator fun Stub<*>.unaryPlus() {
        children.add(this)
    }

    operator fun plusAssign(set: Collection<Stub<*>>) {
        children += set
    }

    fun build() = children
}

internal inline fun buildMembers(block: StubBuilder.() -> Unit): List<Stub<*>> = StubBuilder().let {
    it.block()
    it.build()
}
