/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package greeteer

import example.Clock

class Greeter {
    /**
     * Some docs for the [greet] function
     */
    fun greet() = Clock().let{ "Hello there! THe time is ${it.getTime()}" }
}
