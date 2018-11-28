/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import kotlin.script.experimental.annotations.*

@KotlinScript(fileExtension = "greet.kts")
abstract class GreetScriptTemplate {
    fun greet(subject: String) {
        println("Hello, $subject!")
    }
}