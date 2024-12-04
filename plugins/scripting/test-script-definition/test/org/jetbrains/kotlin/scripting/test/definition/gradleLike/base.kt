/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.definition.gradleLike

open class DefaultKotlinScript {
    fun memberApi1(body: (Int) -> Int = { it }): Int = body(42)
}

open class CompiledKotlinBuildScript : DefaultKotlinScript(), PluginAware

class Project : PluginAware


