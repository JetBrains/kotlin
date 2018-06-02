/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.simple

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.annotations.KotlinScriptEvaluator
import kotlin.script.experimental.jvm.runners.BasicJvmScriptEvaluator

@KotlinScript
@KotlinScriptEvaluator(BasicJvmScriptEvaluator::class)
abstract class MyScript {
//    abstract fun body(vararg args: String): Int
}
