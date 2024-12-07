/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.interop

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class NativeInteropExtension {
    abstract val defFileName: Property<String>
    abstract val usePrebuiltSources: Property<Boolean>
    abstract val commonCompilerArgs: ListProperty<String>
    abstract val cCompilerArgs: ListProperty<String>
    abstract val cppCompilerArgs: ListProperty<String>
    abstract val selfHeaders: ListProperty<String>
    abstract val systemIncludeDirs: ListProperty<String>
    abstract val linkerArgs: ListProperty<String>
    abstract val additionalLinkedStaticLibraries: ListProperty<String>
}