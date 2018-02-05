/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.resolve.maven

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.basic.DefaultScriptSelector
import kotlin.script.experimental.jvm.runners.BasicJvmScriptRunner

@KotlinScript(
    DefaultScriptSelector::class,
    MyConfigurator::class,
    BasicJvmScriptRunner::class
)
abstract class MyScriptWithMavenDeps {
//    abstract fun body(vararg args: String): Int
}
