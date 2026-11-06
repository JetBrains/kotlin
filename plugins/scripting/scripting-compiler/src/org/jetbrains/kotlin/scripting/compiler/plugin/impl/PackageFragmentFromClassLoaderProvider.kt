/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import kotlin.script.experimental.api.ScriptCompilationConfiguration

class PackageFragmentFromClassLoaderProviderExtension(
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val compilerConfiguration: CompilerConfiguration
) : PackageFragmentProviderExtension

