/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.gen.*
import org.jetbrains.kotlin.native.interop.indexer.*

object Plugins {
    fun plugin(pluginName: String?): Plugin = when (pluginName) {
        "org.jetbrains.kotlin.native.interop.skia" ->
            Class.forName("$pluginName.SkiaPlugin").newInstance() as Plugin
        null -> DefaultPlugin
        else -> error("Unexected interop plugin: $pluginName")
    }
}

interface Plugin {
    val name: String
    fun buildNativeIndex(library: NativeLibrary, verbose: Boolean): IndexerResult
    val managedTypePassing: ManagedTypePassing
    val ManagedType.stringRepresentation: String
    fun stubsBuildingContext(stubIrContext: StubIrContext): StubsBuildingContext
}

object DefaultPlugin : Plugin {
    override val name = "Default"
    override fun buildNativeIndex(library: NativeLibrary, verbose: Boolean): IndexerResult =
            buildNativeIndexImpl(library, verbose)
    override val managedTypePassing = ManagedTypePassing()
    override val ManagedType.stringRepresentation get() = error("ManagedType requires non-default interop plugin")
    override fun stubsBuildingContext(stubIrContext: StubIrContext) = StubsBuildingContextImpl(stubIrContext)
}
